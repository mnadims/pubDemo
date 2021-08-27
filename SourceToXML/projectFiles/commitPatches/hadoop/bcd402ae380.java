From bcd402ae380ead1234bfdfc53f485d3fb1391288 Mon Sep 17 00:00:00 2001
From: Andrew Wang <wang@apache.org>
Date: Tue, 18 Nov 2014 10:47:46 -0800
Subject: [PATCH] HADOOP-11312. Fix unit tests to not use uppercase key names.

--
 .../hadoop-common/CHANGES.txt                 |  2 +
 .../hadoop/crypto/key/kms/server/TestKMS.java | 40 ++++++++++---------
 .../hdfs/nfs/nfs3/TestRpcProgramNfs3.java     |  2 +-
 .../hadoop/hdfs/TestEncryptionZones.java      |  6 +--
 .../hdfs/TestEncryptionZonesWithHA.java       |  2 +-
 .../hadoop/hdfs/TestReservedRawPaths.java     |  2 +-
 6 files changed, 29 insertions(+), 25 deletions(-)

diff --git a/hadoop-common-project/hadoop-common/CHANGES.txt b/hadoop-common-project/hadoop-common/CHANGES.txt
index 7fc29a6930a..013353a6e80 100644
-- a/hadoop-common-project/hadoop-common/CHANGES.txt
++ b/hadoop-common-project/hadoop-common/CHANGES.txt
@@ -450,6 +450,8 @@ Release 2.7.0 - UNRELEASED
     HADOOP-11309. System class pattern package.Foo should match
     package.Foo$Bar, too (Gera Shegalov via jlowe)
 
    HADOOP-11312. Fix unit tests to not use uppercase key names. (wang)

 Release 2.6.0 - 2014-11-18
 
   INCOMPATIBLE CHANGES
diff --git a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
index 86e648492a4..86e0516554b 100644
-- a/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
++ b/hadoop-common-project/hadoop-kms/src/test/java/org/apache/hadoop/crypto/key/kms/server/TestKMS.java
@@ -1522,8 +1522,10 @@ public void testDelegationTokenAccess() throws Exception {
     conf.set("hadoop.kms.authentication.kerberos.principal", "HTTP/localhost");
     conf.set("hadoop.kms.authentication.kerberos.name.rules", "DEFAULT");
 
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kA.ALL", "*");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kD.ALL", "*");
    final String keyA = "key_a";
    final String keyD = "key_d";
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + keyA + ".ALL", "*");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + keyD + ".ALL", "*");
 
     writeConf(testDir, conf);
 
@@ -1539,7 +1541,7 @@ public Void call() throws Exception {
 
         try {
           KeyProvider kp = new KMSClientProvider(uri, conf);
          kp.createKey("kA", new KeyProvider.Options(conf));
          kp.createKey(keyA, new KeyProvider.Options(conf));
         } catch (IOException ex) {
           System.out.println(ex.getMessage());
         }
@@ -1560,7 +1562,7 @@ public Void run() throws Exception {
 
         try {
           KeyProvider kp = new KMSClientProvider(uri, conf);
          kp.createKey("kA", new KeyProvider.Options(conf));
          kp.createKey(keyA, new KeyProvider.Options(conf));
         } catch (IOException ex) {
           System.out.println(ex.getMessage());
         }
@@ -1569,7 +1571,7 @@ public Void run() throws Exception {
           @Override
           public Void run() throws Exception {
             KeyProvider kp = new KMSClientProvider(uri, conf);
            kp.createKey("kD", new KeyProvider.Options(conf));
            kp.createKey(keyD, new KeyProvider.Options(conf));
             return null;
           }
         });
@@ -1704,10 +1706,10 @@ public void doProxyUserTest(final boolean kerberos) throws Exception {
     conf.set("hadoop.kms.authentication.kerberos.name.rules", "DEFAULT");
     conf.set("hadoop.kms.proxyuser.client.users", "foo,bar");
     conf.set("hadoop.kms.proxyuser.client.hosts", "*");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kAA.ALL", "client");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kBB.ALL", "foo");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kCC.ALL", "foo1");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kDD.ALL", "bar");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kaa.ALL", "client");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kbb.ALL", "foo");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kcc.ALL", "foo1");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kdd.ALL", "bar");
 
     writeConf(testDir, conf);
 
@@ -1732,7 +1734,7 @@ public Void call() throws Exception {
           @Override
           public Void run() throws Exception {
             final KeyProvider kp = new KMSClientProvider(uri, conf);
            kp.createKey("kAA", new KeyProvider.Options(conf));
            kp.createKey("kaa", new KeyProvider.Options(conf));
 
             // authorized proxyuser
             UserGroupInformation fooUgi =
@@ -1740,7 +1742,7 @@ public Void run() throws Exception {
             fooUgi.doAs(new PrivilegedExceptionAction<Void>() {
               @Override
               public Void run() throws Exception {
                Assert.assertNotNull(kp.createKey("kBB",
                Assert.assertNotNull(kp.createKey("kbb",
                     new KeyProvider.Options(conf)));
                 return null;
               }
@@ -1753,7 +1755,7 @@ public Void run() throws Exception {
               @Override
               public Void run() throws Exception {
                 try {
                  kp.createKey("kCC", new KeyProvider.Options(conf));
                  kp.createKey("kcc", new KeyProvider.Options(conf));
                   Assert.fail();
                 } catch (AuthorizationException ex) {
                   // OK
@@ -1770,7 +1772,7 @@ public Void run() throws Exception {
             barUgi.doAs(new PrivilegedExceptionAction<Void>() {
               @Override
               public Void run() throws Exception {
                Assert.assertNotNull(kp.createKey("kDD",
                Assert.assertNotNull(kp.createKey("kdd",
                     new KeyProvider.Options(conf)));
                 return null;
               }
@@ -1810,9 +1812,9 @@ public void doWebHDFSProxyUserTest(final boolean kerberos) throws Exception {
     conf.set("hadoop.security.kms.client.timeout", "300");
     conf.set("hadoop.kms.proxyuser.client.users", "foo,bar");
     conf.set("hadoop.kms.proxyuser.client.hosts", "*");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kAA.ALL", "foo");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kBB.ALL", "foo1");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kCC.ALL", "bar");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kaa.ALL", "foo");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kbb.ALL", "foo1");
    conf.set(KeyAuthorizationKeyProvider.KEY_ACL + "kcc.ALL", "bar");
 
     writeConf(testDir, conf);
 
@@ -1844,7 +1846,7 @@ public Void run() throws Exception {
               @Override
               public Void run() throws Exception {
                 KeyProvider kp = new KMSClientProvider(uri, conf);
                Assert.assertNotNull(kp.createKey("kAA",
                Assert.assertNotNull(kp.createKey("kaa",
                     new KeyProvider.Options(conf)));
                 return null;
               }
@@ -1858,7 +1860,7 @@ public Void run() throws Exception {
               public Void run() throws Exception {
                 try {
                   KeyProvider kp = new KMSClientProvider(uri, conf);
                  kp.createKey("kBB", new KeyProvider.Options(conf));
                  kp.createKey("kbb", new KeyProvider.Options(conf));
                   Assert.fail();
                 } catch (Exception ex) {
                   Assert.assertTrue(ex.getMessage(), ex.getMessage().contains("Forbidden"));
@@ -1874,7 +1876,7 @@ public Void run() throws Exception {
               @Override
               public Void run() throws Exception {
                 KeyProvider kp = new KMSClientProvider(uri, conf);
                Assert.assertNotNull(kp.createKey("kCC",
                Assert.assertNotNull(kp.createKey("kcc",
                     new KeyProvider.Options(conf)));
                 return null;
               }
diff --git a/hadoop-hdfs-project/hadoop-hdfs-nfs/src/test/java/org/apache/hadoop/hdfs/nfs/nfs3/TestRpcProgramNfs3.java b/hadoop-hdfs-project/hadoop-hdfs-nfs/src/test/java/org/apache/hadoop/hdfs/nfs/nfs3/TestRpcProgramNfs3.java
index 8b895eb4cec..10a175cb9b0 100644
-- a/hadoop-hdfs-project/hadoop-hdfs-nfs/src/test/java/org/apache/hadoop/hdfs/nfs/nfs3/TestRpcProgramNfs3.java
++ b/hadoop-hdfs-project/hadoop-hdfs-nfs/src/test/java/org/apache/hadoop/hdfs/nfs/nfs3/TestRpcProgramNfs3.java
@@ -115,7 +115,7 @@
   static SecurityHandler securityHandler;
   static SecurityHandler securityHandlerUnpriviledged;
   static String testdir = "/tmp";
  private static final String TEST_KEY = "testKey";
  private static final String TEST_KEY = "test_key";
   private static FileSystemTestHelper fsHelper;
   private static File testRootDir;
 
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZones.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZones.java
index 1f98a623dfd..603bf6e0cb2 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZones.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZones.java
@@ -120,7 +120,7 @@
   protected HdfsAdmin dfsAdmin;
   protected DistributedFileSystem fs;
   private File testRootDir;
  protected final String TEST_KEY = "testKey";
  protected final String TEST_KEY = "test_key";
 
   protected FileSystemTestWrapper fsWrapper;
   protected FileContextTestWrapper fcWrapper;
@@ -985,7 +985,7 @@ public void doCleanup() throws Exception {
 
     // Test when the parent directory becomes a different EZ
     fsWrapper.mkdir(zone1, FsPermission.getDirDefault(), true);
    final String otherKey = "otherKey";
    final String otherKey = "other_key";
     DFSTestUtil.createKey(otherKey, cluster, conf);
     dfsAdmin.createEncryptionZone(zone1, TEST_KEY);
 
@@ -1005,7 +1005,7 @@ public void doCleanup() throws Exception {
 
     // Test that the retry limit leads to an error
     fsWrapper.mkdir(zone1, FsPermission.getDirDefault(), true);
    final String anotherKey = "anotherKey";
    final String anotherKey = "another_key";
     DFSTestUtil.createKey(anotherKey, cluster, conf);
     dfsAdmin.createEncryptionZone(zone1, anotherKey);
     String keyToUse = otherKey;
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZonesWithHA.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZonesWithHA.java
index 04977d4597d..3339f167c23 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZonesWithHA.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestEncryptionZonesWithHA.java
@@ -48,7 +48,7 @@
   private FileSystemTestHelper fsHelper;
   private File testRootDir;
 
  private final String TEST_KEY = "testKey";
  private final String TEST_KEY = "test_key";
 
 
   @Before
diff --git a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestReservedRawPaths.java b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestReservedRawPaths.java
index cc497ac63b7..13381335f09 100644
-- a/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestReservedRawPaths.java
++ b/hadoop-hdfs-project/hadoop-hdfs/src/test/java/org/apache/hadoop/hdfs/TestReservedRawPaths.java
@@ -56,7 +56,7 @@
   private MiniDFSCluster cluster;
   private HdfsAdmin dfsAdmin;
   private DistributedFileSystem fs;
  private final String TEST_KEY = "testKey";
  private final String TEST_KEY = "test_key";
 
   protected FileSystemTestWrapper fsWrapper;
   protected FileContextTestWrapper fcWrapper;
- 
2.19.1.windows.1

