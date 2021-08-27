From f342cd14df8c4d7d985473cdbfb6db7792a33cd6 Mon Sep 17 00:00:00 2001
From: Shalin Shekhar Mangar <shalin@apache.org>
Date: Tue, 30 Dec 2008 07:19:26 +0000
Subject: [PATCH] SOLR-942 -- Different Fields with same column and different
 names do not work

git-svn-id: https://svn.apache.org/repos/asf/lucene/solr/trunk@730059 13f79535-47bb-0310-9956-ffa450edef68
--
 .../org/apache/solr/handler/dataimport/DataConfig.java   | 9 ++++++---
 .../org/apache/solr/handler/dataimport/DocBuilder.java   | 9 +++++----
 .../apache/solr/handler/dataimport/TestDocBuilder.java   | 7 ++++---
 3 files changed, 15 insertions(+), 10 deletions(-)

diff --git a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
index f4f5aa7e40c..4f346293be5 100644
-- a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
++ b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DataConfig.java
@@ -100,7 +100,7 @@ public class DataConfig {
 
     public Script script;
 
    public Map<String, Field> colNameVsField;
    public Map<String, List<Field>> colNameVsField;
 
     public Entity() {
     }
@@ -114,11 +114,14 @@ public class DataConfig {
       allAttributes = getAllAttributes(element);
       List<Element> n = getChildNodes(element, "field");
       fields = new ArrayList<Field>();
      colNameVsField = new HashMap<String, Field>();
      colNameVsField = new HashMap<String, List<Field>>();
       for (Element elem : n)  {
         Field field = new Field(elem);
         fields.add(field);
        colNameVsField.put(field.column, field);
        List<Field> l = colNameVsField.get(field.column);
        if(l == null) l = new ArrayList<Field>();
        l.add(field);
        colNameVsField.put(field.column, l);
       }
       n = getChildNodes(element, "entity");
       if (!n.isEmpty())
diff --git a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
index a2542379975..4ba5441dabc 100644
-- a/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
++ b/contrib/dataimporthandler/src/main/java/org/apache/solr/handler/dataimport/DocBuilder.java
@@ -378,7 +378,7 @@ public class DocBuilder {
         // All fields starting with $ are special values and don't need to be added
         continue;
       }
      DataConfig.Field field = entity.colNameVsField.get(key);
      List<DataConfig.Field> field = entity.colNameVsField.get(key);
       if (field == null && dataImporter.getSchema() != null) {
         // This can be a dynamic field or a field which does not have an entry in data-config ( an implicit field)
         SchemaField sf = dataImporter.getSchema().getFieldOrNull(key);
@@ -390,11 +390,12 @@ public class DocBuilder {
         }
         //else do nothing. if we add it it may fail
       } else {
        if (field != null && field.toWrite) {
          addFieldToDoc(entry.getValue(), field.getName(), field.boost, field.multiValued, doc);
        if (field != null ) {
          for (DataConfig.Field f : field) {
            if(f.toWrite) addFieldToDoc(entry.getValue(), f.getName(), f.boost, f.multiValued, doc);
          }
         }
       }

     }
   }
 
diff --git a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
index 7686a86571e..b454fa5a2b7 100644
-- a/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
++ b/contrib/dataimporthandler/src/test/java/org/apache/solr/handler/dataimport/TestDocBuilder.java
@@ -135,9 +135,9 @@ public class TestDocBuilder {
         Map<String, Object> map = (Map<String, Object>) l.get(i);
         SolrInputDocument doc = swi.docs.get(i);
         for (Map.Entry<String, Object> entry : map.entrySet()) {
          Assert.assertEquals(entry.getValue(), doc.getFieldValue(entry
                  .getKey()));
          Assert.assertEquals(entry.getValue(), doc.getFieldValue(entry.getKey()));
         }
        Assert.assertEquals(map.get("desc"), doc.getFieldValue("desc_s"));
       }
       Assert.assertEquals(1, di.getDocBuilder().importStatistics.queryCount
               .get());
@@ -182,7 +182,8 @@ public class TestDocBuilder {
           + "    <document name=\"X\" >\n"
           + "        <entity name=\"x\" query=\"select * from x\">\n"
           + "          <field column=\"id\"/>\n"
          + "          <field column=\"desc\"/>\n" + "        </entity>\n"
          + "          <field column=\"desc\"/>\n"
          + "          <field column=\"desc\" name=\"desc_s\" />" + "        </entity>\n"
           + "    </document>\n" + "</dataConfig>";
 
 }
- 
2.19.1.windows.1

