From 60f161fd9b1792dbb7322f19c6879cefd95b75cb Mon Sep 17 00:00:00 2001
From: Yonik Seeley <yonik@apache.org>
Date: Thu, 19 Mar 2015 16:11:36 +0000
Subject: [PATCH] SOLR-7262: fix broken thread safety for request handler
 registry introduced by SOLR-7073

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1667799 13f79535-47bb-0310-9956-ffa450edef68
--
 .../java/org/apache/solr/core/PluginBag.java  | 23 +++++++++++++++----
 .../org/apache/solr/core/RequestHandlers.java |  3 ++-
 2 files changed, 20 insertions(+), 6 deletions(-)

diff --git a/solr/core/src/java/org/apache/solr/core/PluginBag.java b/solr/core/src/java/org/apache/solr/core/PluginBag.java
index 1a4141203e4..c787cd863a9 100644
-- a/solr/core/src/java/org/apache/solr/core/PluginBag.java
++ b/solr/core/src/java/org/apache/solr/core/PluginBag.java
@@ -27,6 +27,7 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
 
 import org.apache.lucene.analysis.util.ResourceLoader;
 import org.apache.lucene.analysis.util.ResourceLoaderAware;
@@ -49,22 +50,33 @@ import org.slf4j.LoggerFactory;
 public class PluginBag<T> implements AutoCloseable {
   public static Logger log = LoggerFactory.getLogger(PluginBag.class);
 
  private Map<String, PluginHolder<T>> registry = new HashMap<>();
  private Map<String, PluginHolder<T>> immutableRegistry = Collections.unmodifiableMap(registry);
  private final Map<String, PluginHolder<T>> registry;
  private final Map<String, PluginHolder<T>> immutableRegistry;
   private String def;
  private Class klass;
  private final Class klass;
   private SolrCore core;
  private SolrConfig.SolrPluginInfo meta;
  private final SolrConfig.SolrPluginInfo meta;
 
  public PluginBag(Class<T> klass, SolrCore core) {
  /** Pass needThreadSafety=true if plugins can be added and removed concurrently with lookups. */
  public PluginBag(Class<T> klass, SolrCore core, boolean needThreadSafety) {
     this.core = core;
     this.klass = klass;
    // TODO: since reads will dominate writes, we could also think about creating a new instance of a map each time it changes.
    // Not sure how much benefit this would have over ConcurrentHashMap though
    // We could also perhaps make this constructor into a factory method to return different implementations depending on thread safety needs.
    this.registry = needThreadSafety ? new ConcurrentHashMap<>() : new HashMap<>();
    this.immutableRegistry = Collections.unmodifiableMap(registry);
     meta = SolrConfig.classVsSolrPluginInfo.get(klass.getName());
     if (meta == null) {
       throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unknown Plugin : " + klass.getName());
     }
   }
 
  /** Constructs a non-threadsafe plugin registry */
  public PluginBag(Class<T> klass, SolrCore core) {
    this(klass, core, false);
  }

   static void initInstance(Object inst, PluginInfo info, SolrCore core) {
     if (inst instanceof PluginInfoInitialized) {
       ((PluginInfoInitialized) inst).init(info);
@@ -97,6 +109,7 @@ public class PluginBag<T> implements AutoCloseable {
   }
 
   boolean alias(String src, String target) {
    if (src == null) return false;
     PluginHolder<T> a = registry.get(src);
     if (a == null) return false;
     PluginHolder<T> b = registry.get(target);
diff --git a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
index 44fa89d5e0d..8e093f0b553 100644
-- a/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
++ b/solr/core/src/java/org/apache/solr/core/RequestHandlers.java
@@ -57,7 +57,8 @@ public final class RequestHandlers {
   
   public RequestHandlers(SolrCore core) {
       this.core = core;
    handlers =  new PluginBag<>(SolrRequestHandler.class, core);
    // we need a thread safe registry since methods like register are currently documented to be thread safe.
    handlers =  new PluginBag<>(SolrRequestHandler.class, core, true);
   }
 
   /**
- 
2.19.1.windows.1

