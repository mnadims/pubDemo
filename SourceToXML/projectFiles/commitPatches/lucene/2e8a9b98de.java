From 2e8a9b98de9a671722b6e2a67f2e3b8750842468 Mon Sep 17 00:00:00 2001
From: Robert Muir <rmuir@apache.org>
Date: Wed, 26 May 2010 05:43:00 +0000
Subject: [PATCH] LUCENE-2458: queryparser turns all CJK queries into phrase
 queries

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@948326 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |  4 +
 lucene/build.xml                              |  4 -
 .../vectorhighlight/FieldQueryTest.java       |  4 +-
 .../ext/ExtendableQueryParser.java            |  4 +-
 .../precedence/PrecedenceQueryParser.java     | 20 ++--
 .../precedence/PrecedenceQueryParser.jj       | 22 +++--
 .../standard/QueryParserWrapper.java          |  2 +-
 .../AnalyzerQueryNodeProcessor.java           | 11 ++-
 .../precedence/TestPrecedenceQueryParser.java | 93 +++++++++++++++++-
 .../queryParser/standard/TestQPHelper.java    | 91 +++++++++++++++++-
 .../standard/TestQueryParserWrapper.java      | 91 +++++++++++++++++-
 .../queryParser/MultiFieldQueryParser.java    | 29 +++++-
 .../lucene/queryParser/QueryParser.java       | 36 ++++---
 .../apache/lucene/queryParser/QueryParser.jj  | 24 +++--
 .../lucene/queryParser/TestMultiAnalyzer.java | 12 +--
 .../lucene/queryParser/TestQueryParser.java   | 94 ++++++++++++++++++-
 .../search/ExtendedDismaxQParserPlugin.java   |  4 +-
 .../apache/solr/search/SolrQueryParser.java   |  4 +-
 .../org/apache/solr/util/SolrPluginUtils.java |  6 +-
 19 files changed, 474 insertions(+), 81 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index 82d6001beec..db3bb077f24 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -406,6 +406,10 @@ Bug fixes
   lock (previously we only released on IOException).  (Tamas Cservenak
   via Mike McCandless)
 
* LUCENE-2458: QueryParser no longer automatically forms phrase queries,
  assuming whitespace tokenization. Previously all CJK queries, for example,
  would be turned into phrase queries.  (Robert Muir)

 New features
 
 * LUCENE-2128: Parallelized fetching document frequencies during weight
diff --git a/lucene/build.xml b/lucene/build.xml
index a9d301c4f96..4a0b0f5c453 100644
-- a/lucene/build.xml
++ b/lucene/build.xml
@@ -490,10 +490,6 @@ The source distribution does not contain sources of the previous Lucene Java ver
   <!-- ================================================================== -->
   <target name="clean-javacc">
     <delete>
      <fileset dir="src/java/org/apache/lucene/analysis/standard" includes="*.java">
        <containsregexp expression="Generated.*By.*JavaCC"/>
        <exclude name="ParseException.java"/>
      </fileset>
       <fileset dir="src/java/org/apache/lucene/queryParser" includes="*.java">
         <containsregexp expression="Generated.*By.*JavaCC"/>
       </fileset>
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
index cb73765fcaf..42924fdc8c9 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/FieldQueryTest.java
@@ -55,7 +55,7 @@ public class FieldQueryTest extends AbstractTestCase {
   }
 
   public void testFlattenTermAndPhrase2gram() throws Exception {
    Query query = paB.parse( "AA AND BCD OR EFGH" );
    Query query = paB.parse( "AA AND \"BCD\" OR \"EFGH\"" );
     FieldQuery fq = new FieldQuery( query, true, true );
     Set<Query> flatQueries = new HashSet<Query>();
     fq.flatten( query, flatQueries );
@@ -679,7 +679,7 @@ public class FieldQueryTest extends AbstractTestCase {
   }
   
   public void testQueryPhraseMapOverlap2gram() throws Exception {
    Query query = paB.parse( "abc AND bcd" );
    Query query = paB.parse( "\"abc\" AND \"bcd\"" );
     
     // phraseHighlight = true, fieldMatch = true
     FieldQuery fq = new FieldQuery( query, true, true );
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
index 1533d11d5bd..6592c60afef 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/ext/ExtendableQueryParser.java
@@ -126,7 +126,7 @@ public class ExtendableQueryParser extends QueryParser {
   }
 
   @Override
  protected Query getFieldQuery(final String field, final String queryText)
  protected Query getFieldQuery(final String field, final String queryText, boolean quoted)
       throws ParseException {
     final Pair<String,String> splitExtensionField = this.extensions
         .splitExtensionField(defaultField, field);
@@ -136,7 +136,7 @@ public class ExtendableQueryParser extends QueryParser {
       return extension.parse(new ExtensionQuery(this, splitExtensionField.cur,
           queryText));
     }
    return super.getFieldQuery(field, queryText);
    return super.getFieldQuery(field, queryText, quoted);
   }
 
 }
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
index b76ddf0d3c5..57ecaebf475 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.java
@@ -299,7 +299,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -330,15 +330,19 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
       source.restoreState(list.get(0));
       return new TermQuery(new Term(field, termAtt.term()));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = new BooleanQuery();
          BooleanQuery q = new BooleanQuery(positionCount == 1);

          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < list.size(); i++) {
             source.restoreState(list.get(i));
             TermQuery currentQuery = new TermQuery(
                 new Term(field, termAtt.term()));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -371,7 +375,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
   }
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -379,7 +383,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -847,7 +851,7 @@ public class PrecedenceQueryParser implements PrecedenceQueryParserConstants {
          }
          q = getFuzzyQuery(field, termImage, fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, false);
        }
       break;
     case RANGEIN_START:
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
index 9cd21242042..9b2eba0ba85 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/precedence/PrecedenceQueryParser.jj
@@ -127,7 +127,7 @@ public class PrecedenceQueryParser {
   Locale locale = Locale.getDefault();
 
   static enum Operator { OR, AND }

  
   /** Constructs a query parser.
    *  @param f  the default field for query terms.
    *  @param a   used to find terms in the query text.
@@ -323,7 +323,7 @@ public class PrecedenceQueryParser {
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -354,15 +354,19 @@ public class PrecedenceQueryParser {
       source.restoreState(list.get(0));
       return new TermQuery(new Term(field, termAtt.term()));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = new BooleanQuery();
          BooleanQuery q = new BooleanQuery(positionCount == 1);
          
          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;
 
           for (int i = 0; i < list.size(); i++) {
             source.restoreState(list.get(i));
             TermQuery currentQuery = new TermQuery(
                 new Term(field, termAtt.term()));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -395,7 +399,7 @@ public class PrecedenceQueryParser {
   }
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -403,7 +407,7 @@ public class PrecedenceQueryParser {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -830,7 +834,7 @@ Query Term(String field) : {
        	 }
          q = getFuzzyQuery(field, termImage, fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, false);
        }
      }
      | ( <RANGEIN_START> ( goop1=<RANGEIN_GOOP>|goop1=<RANGEIN_QUOTED> )
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
index a5783d72dc7..43ee0c67139 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/QueryParserWrapper.java
@@ -451,7 +451,7 @@ public class QueryParserWrapper {
   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)
  protected Query getFieldQuery(String field, String queryText, boolean quoted)
       throws ParseException {
     throw new UnsupportedOperationException();
   }
diff --git a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
index 7be5c9afafd..2666f291dc5 100644
-- a/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
++ b/lucene/contrib/queryparser/src/java/org/apache/lucene/queryParser/standard/processors/AnalyzerQueryNodeProcessor.java
@@ -36,6 +36,7 @@ import org.apache.lucene.queryParser.core.nodes.GroupQueryNode;
 import org.apache.lucene.queryParser.core.nodes.NoTokenFoundQueryNode;
 import org.apache.lucene.queryParser.core.nodes.ParametricQueryNode;
 import org.apache.lucene.queryParser.core.nodes.QueryNode;
import org.apache.lucene.queryParser.core.nodes.QuotedFieldQueryNode;
 import org.apache.lucene.queryParser.core.nodes.TextableQueryNode;
 import org.apache.lucene.queryParser.core.nodes.TokenizedPhraseQueryNode;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
@@ -187,8 +188,8 @@ public class AnalyzerQueryNodeProcessor extends QueryNodeProcessorImpl {
 
         return fieldNode;
 
      } else if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      } else if (severalTokensAtSamePosition || !(node instanceof QuotedFieldQueryNode)) {
        if (positionCount == 1 || !(node instanceof QuotedFieldQueryNode)) {
           // no phrase query:
           LinkedList<QueryNode> children = new LinkedList<QueryNode>();
 
@@ -206,9 +207,11 @@ public class AnalyzerQueryNodeProcessor extends QueryNodeProcessorImpl {
             children.add(new FieldQueryNode(field, term, -1, -1));
 
           }

          return new GroupQueryNode(
          if (positionCount == 1)
            return new GroupQueryNode(
               new StandardBooleanQueryNode(children, true));
          else
            return new StandardBooleanQueryNode(children, false);
 
         } else {
           // phrase query:
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
index b0907db3b4e..110ff05e1d4 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/precedence/TestPrecedenceQueryParser.java
@@ -23,9 +23,13 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.document.DateTools;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.PhraseQuery;
@@ -37,6 +41,7 @@ import org.apache.lucene.search.WildcardQuery;
 import org.apache.lucene.util.LocalizedTestCase;
 import org.apache.lucene.util.automaton.BasicAutomata;
 import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.RegExp;
 
 import java.io.IOException;
 import java.io.Reader;
@@ -260,6 +265,90 @@ public class TestPrecedenceQueryParser extends LocalizedTestCase {
     assertQueryEquals(".NET", a, ".NET");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }
  
  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }

  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   public void testSlop() throws Exception {
     assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
     assertQueryEquals("\"term germ\"~2 flork", null, "\"term germ\"~2 flork");
@@ -353,11 +442,11 @@ public class TestPrecedenceQueryParser extends LocalizedTestCase {
     assertQueryEquals("term -stop term", qpAnalyzer, "term term");
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
                      "term \"phrase1 phrase2\" term");
                      "term (phrase1 phrase2) term");
     // note the parens in this next assertion differ from the original
     // QueryParser behavior
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
                      "(+term -\"phrase1 phrase2\") term");
                      "(+term -(phrase1 phrase2)) term");
     assertQueryEquals("stop", qpAnalyzer, "");
     assertQueryEquals("stop OR stop AND stop", qpAnalyzer, "");
     assertTrue(getQuery("term term term", qpAnalyzer) instanceof BooleanQuery);
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
index addbca26476..7da1188b4a0 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQPHelper.java
@@ -37,6 +37,8 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -57,6 +59,7 @@ import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
 import org.apache.lucene.queryParser.standard.config.DefaultOperatorAttribute.Operator;
 import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.IndexSearcher;
@@ -333,6 +336,90 @@ public class TestQPHelper extends LocalizedTestCase {
     assertQueryEqualsAllowLeadingWildcard("??\u3000??\u3000??", null, "??\u0020??\u0020??");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }
  
  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }

  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   public void testSimple() throws Exception {
     assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
     assertQueryEquals("term term term", null, "term term term");
@@ -531,10 +618,10 @@ public class TestQPHelper extends LocalizedTestCase {
 
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
        "term \"phrase1 phrase2\" term");
        "term phrase1 phrase2 term");
 
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
        "+term -\"phrase1 phrase2\" term");
        "+term -(phrase1 phrase2) term");
 
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
diff --git a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
index b3a28dbe1b0..6646335d798 100644
-- a/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
++ b/lucene/contrib/queryparser/src/test/org/apache/lucene/queryParser/standard/TestQueryParserWrapper.java
@@ -35,6 +35,8 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermAttribute;
 import org.apache.lucene.document.DateField;
@@ -53,6 +55,7 @@ import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorImpl;
 import org.apache.lucene.queryParser.core.processors.QueryNodeProcessorPipeline;
 import org.apache.lucene.queryParser.standard.nodes.WildcardQueryNode;
 import org.apache.lucene.queryParser.standard.processors.WildcardQueryNodeProcessor;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.FuzzyQuery;
 import org.apache.lucene.search.IndexSearcher;
@@ -325,6 +328,90 @@ public class TestQueryParserWrapper extends LocalizedTestCase {
     assertQueryEqualsAllowLeadingWildcard("??\u3000??\u3000??", null, "??\u0020??\u0020??");
   }
 
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }
  
  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }

  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   public void testSimple() throws Exception {
     assertQueryEquals("\"term germ\"~2", null, "\"term germ\"~2");
     assertQueryEquals("term term term", null, "term term term");
@@ -530,10 +617,10 @@ public class TestQueryParserWrapper extends LocalizedTestCase {
 
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
        "term \"phrase1 phrase2\" term");
        "term phrase1 phrase2 term");
 
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
        "+term -\"phrase1 phrase2\" term");
        "+term -(phrase1 phrase2) term");
 
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
diff --git a/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java b/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
index 90f1b5fa755..284e35c1806 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
++ b/lucene/src/java/org/apache/lucene/queryParser/MultiFieldQueryParser.java
@@ -101,7 +101,7 @@ public class MultiFieldQueryParser extends QueryParser
     if (field == null) {
       List<BooleanClause> clauses = new ArrayList<BooleanClause>();
       for (int i = 0; i < fields.length; i++) {
        Query q = super.getFieldQuery(fields[i], queryText);
        Query q = super.getFieldQuery(fields[i], queryText, true);
         if (q != null) {
           //If the user passes a map of boosts
           if (boosts != null) {
@@ -119,7 +119,7 @@ public class MultiFieldQueryParser extends QueryParser
         return null;
       return getBooleanQuery(clauses, true);
     }
    Query q = super.getFieldQuery(field, queryText);
    Query q = super.getFieldQuery(field, queryText, true);
     applySlop(q,slop);
     return q;
   }
@@ -134,8 +134,29 @@ public class MultiFieldQueryParser extends QueryParser
   
 
   @Override
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
    return getFieldQuery(field, queryText, 0);
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
    if (field == null) {
      List<BooleanClause> clauses = new ArrayList<BooleanClause>();
      for (int i = 0; i < fields.length; i++) {
        Query q = super.getFieldQuery(fields[i], queryText, quoted);
        if (q != null) {
          //If the user passes a map of boosts
          if (boosts != null) {
            //Get the boost from the map and apply them
            Float boost = boosts.get(fields[i]);
            if (boost != null) {
              q.setBoost(boost.floatValue());
            }
          }
          clauses.add(new BooleanClause(q, BooleanClause.Occur.SHOULD));
        }
      }
      if (clauses.size() == 0)  // happens for stopwords
        return null;
      return getBooleanQuery(clauses, true);
    }
    Query q = super.getFieldQuery(field, queryText, quoted);
    return q;
   }
 
 
diff --git a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
index 9fed418e9a7..660d066620c 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
++ b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.java
@@ -150,6 +150,8 @@ public class QueryParser implements QueryParserConstants {
   // for use when constructing RangeQuerys.
   Collator rangeCollator = null;
 
  private Version matchVersion;

   /** The default operator for parsing queries. 
    * Use {@link QueryParser#setDefaultOperator} to change it.
    */
@@ -162,6 +164,7 @@ public class QueryParser implements QueryParserConstants {
    */
   public QueryParser(Version matchVersion, String f, Analyzer a) {
     this(new FastCharStream(new StringReader("")));
    this.matchVersion = matchVersion;
     analyzer = a;
     field = f;
     if (matchVersion.onOrAfter(Version.LUCENE_29)) {
@@ -506,11 +509,10 @@ public class QueryParser implements QueryParserConstants {
       throw new RuntimeException("Clause cannot be both required and prohibited");
   }
 

   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -587,10 +589,14 @@ public class QueryParser implements QueryParserConstants {
       }
       return newTermQuery(new Term(field, term));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = newBooleanQuery(true);
          BooleanQuery q = newBooleanQuery(positionCount == 1);

          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ?
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < numTokens; i++) {
             String term = null;
             try {
@@ -603,7 +609,7 @@ public class QueryParser implements QueryParserConstants {
 
             Query currentQuery = newTermQuery(
                 new Term(field, term));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -682,7 +688,7 @@ public class QueryParser implements QueryParserConstants {
 
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -690,7 +696,7 @@ public class QueryParser implements QueryParserConstants {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -1343,7 +1349,7 @@ public class QueryParser implements QueryParserConstants {
          }
          q = getFuzzyQuery(field, termImage,fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, !matchVersion.onOrAfter(Version.LUCENE_31));
        }
       break;
     case RANGEIN_START:
@@ -1512,6 +1518,12 @@ public class QueryParser implements QueryParserConstants {
     finally { jj_save(0, xla); }
   }
 
  private boolean jj_3R_3() {
    if (jj_scan_token(STAR)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

   private boolean jj_3R_2() {
     if (jj_scan_token(TERM)) return true;
     if (jj_scan_token(COLON)) return true;
@@ -1528,12 +1540,6 @@ public class QueryParser implements QueryParserConstants {
     return false;
   }
 
  private boolean jj_3R_3() {
    if (jj_scan_token(STAR)) return true;
    if (jj_scan_token(COLON)) return true;
    return false;
  }

   /** Generated Token Manager. */
   public QueryParserTokenManager token_source;
   /** Current token. */
diff --git a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
index fa4eed3cbc0..c6bfa37c549 100644
-- a/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
++ b/lucene/src/java/org/apache/lucene/queryParser/QueryParser.jj
@@ -174,6 +174,8 @@ public class QueryParser {
   // for use when constructing RangeQuerys.
   Collator rangeCollator = null;
 
  private Version matchVersion;

   /** The default operator for parsing queries. 
    * Use {@link QueryParser#setDefaultOperator} to change it.
    */
@@ -186,6 +188,7 @@ public class QueryParser {
    */
   public QueryParser(Version matchVersion, String f, Analyzer a) {
     this(new FastCharStream(new StringReader("")));
    this.matchVersion = matchVersion;
     analyzer = a;
     field = f;
     if (matchVersion.onOrAfter(Version.LUCENE_29)) {
@@ -530,11 +533,10 @@ public class QueryParser {
       throw new RuntimeException("Clause cannot be both required and prohibited");
   }
 

   /**
    * @exception ParseException throw in overridden method to disallow
    */
  protected Query getFieldQuery(String field, String queryText)  throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted)  throws ParseException {
     // Use the analyzer to get all the tokens, and then build a TermQuery,
     // PhraseQuery, or nothing based on the term count
 
@@ -611,10 +613,14 @@ public class QueryParser {
       }
       return newTermQuery(new Term(field, term));
     } else {
      if (severalTokensAtSamePosition) {
        if (positionCount == 1) {
      if (severalTokensAtSamePosition || !quoted) {
        if (positionCount == 1 || !quoted) {
           // no phrase query:
          BooleanQuery q = newBooleanQuery(true);
          BooleanQuery q = newBooleanQuery(positionCount == 1);
          
          BooleanClause.Occur occur = positionCount > 1 && operator == AND_OPERATOR ? 
            BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD;

           for (int i = 0; i < numTokens; i++) {
             String term = null;
             try {
@@ -627,7 +633,7 @@ public class QueryParser {
 
             Query currentQuery = newTermQuery(
                 new Term(field, term));
            q.add(currentQuery, BooleanClause.Occur.SHOULD);
            q.add(currentQuery, occur);
           }
           return q;
         }
@@ -706,7 +712,7 @@ public class QueryParser {
 
 
   /**
   * Base implementation delegates to {@link #getFieldQuery(String,String)}.
   * Base implementation delegates to {@link #getFieldQuery(String,String,boolean)}.
    * This method may be overridden, for example, to return
    * a SpanNearQuery instead of a PhraseQuery.
    *
@@ -714,7 +720,7 @@ public class QueryParser {
    */
   protected Query getFieldQuery(String field, String queryText, int slop)
         throws ParseException {
    Query query = getFieldQuery(field, queryText);
    Query query = getFieldQuery(field, queryText, true);
 
     if (query instanceof PhraseQuery) {
       ((PhraseQuery) query).setSlop(slop);
@@ -1314,7 +1320,7 @@ Query Term(String field) : {
        	 }
        	 q = getFuzzyQuery(field, termImage,fms);
        } else {
         q = getFieldQuery(field, termImage);
         q = getFieldQuery(field, termImage, !matchVersion.onOrAfter(Version.LUCENE_31));
        }
      }
      | ( <RANGEIN_START> ( goop1=<RANGEIN_GOOP>|goop1=<RANGEIN_QUOTED> )
diff --git a/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java b/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
index 69979a4f797..061086cccde 100644
-- a/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
++ b/lucene/src/test/org/apache/lucene/queryParser/TestMultiAnalyzer.java
@@ -104,9 +104,9 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
     // direct call to (super's) getFieldQuery to demonstrate differnce
     // between phrase and multiphrase with modified default slop
     assertEquals("\"foo bar\"~99",
                 qp.getSuperFieldQuery("","foo bar").toString());
                 qp.getSuperFieldQuery("","foo bar", true).toString());
     assertEquals("\"(multi multi2) bar\"~99",
                 qp.getSuperFieldQuery("","multi bar").toString());
                 qp.getSuperFieldQuery("","multi bar", true).toString());
 
     
     // ask sublcass to parse phrase with modified default slop
@@ -243,15 +243,15 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
         }
 
         /** expose super's version */
        public Query getSuperFieldQuery(String f, String t) 
        public Query getSuperFieldQuery(String f, String t, boolean quoted) 
             throws ParseException {
            return super.getFieldQuery(f,t);
            return super.getFieldQuery(f,t,quoted);
         }
         /** wrap super's version */
         @Override
        protected Query getFieldQuery(String f, String t)
        protected Query getFieldQuery(String f, String t, boolean quoted)
             throws ParseException {
            return new DumbQueryWrapper(getSuperFieldQuery(f,t));
            return new DumbQueryWrapper(getSuperFieldQuery(f,t,quoted));
         }
     }
     
diff --git a/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java b/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
index 3691b8775a9..ecdd0abf081 100644
-- a/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
++ b/lucene/src/test/org/apache/lucene/queryParser/TestQueryParser.java
@@ -34,6 +34,7 @@ import org.apache.lucene.analysis.MockTokenFilter;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.DateField;
@@ -44,6 +45,7 @@ import org.apache.lucene.index.IndexWriter;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.Term;
 import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.BooleanClause;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.MultiTermQuery;
 import org.apache.lucene.search.FuzzyQuery;
@@ -249,6 +251,90 @@ public class TestQueryParser extends LocalizedTestCase {
 	 assertQueryEquals("用語\u3000用語\u3000用語", null, "用語\u0020用語\u0020用語");
   }
   
  //individual CJK chars as terms, like StandardAnalyzer
  private class SimpleCJKTokenizer extends Tokenizer {
    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    
    public SimpleCJKTokenizer(Reader input) {
      super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
      int ch = input.read();
      if (ch < 0)
        return false;
      clearAttributes();
      termAtt.setEmpty().append((char) ch);
      return true;
    }
  }
  
  private class SimpleCJKAnalyzer extends Analyzer {
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    }
  }

  public void testCJKTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国", analyzer));
  }
  
  public void testCJKBoostedTerm() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    BooleanQuery expected = new BooleanQuery();
    expected.setBoost(0.5f);
    expected.add(new TermQuery(new Term("field", "中")), BooleanClause.Occur.SHOULD);
    expected.add(new TermQuery(new Term("field", "国")), BooleanClause.Occur.SHOULD);
    
    assertEquals(expected, getQuery("中国^0.5", analyzer));
  }
  
  public void testCJKPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"", analyzer));
  }
  
  public void testCJKBoostedPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer();
    
    PhraseQuery expected = new PhraseQuery();
    expected.setBoost(0.5f);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"^0.5", analyzer));
  }
  
  public void testCJKSloppyPhrase() throws Exception {
    // individual CJK chars as terms
    Analyzer analyzer = new SimpleCJKAnalyzer(); 
    
    PhraseQuery expected = new PhraseQuery();
    expected.setSlop(3);
    expected.add(new Term("field", "中"));
    expected.add(new Term("field", "国"));
    
    assertEquals(expected, getQuery("\"中国\"~3", analyzer));
  }

   public void testSimple() throws Exception {
     assertQueryEquals("term term term", null, "term term term");
     assertQueryEquals("türm term term", new MockAnalyzer(), "türm term term");
@@ -437,9 +523,9 @@ public class TestQueryParser extends LocalizedTestCase {
     
     assertQueryEquals("drop AND stop AND roll", qpAnalyzer, "+drop +roll");
     assertQueryEquals("term phrase term", qpAnalyzer,
                      "term \"phrase1 phrase2\" term");
                      "term (phrase1 phrase2) term");
     assertQueryEquals("term AND NOT phrase term", qpAnalyzer,
                      "+term -\"phrase1 phrase2\" term");
                      "+term -(phrase1 phrase2) term");
     assertQueryEquals("stop^3", qpAnalyzer, "");
     assertQueryEquals("stop", qpAnalyzer, "");
     assertQueryEquals("(stop)^3", qpAnalyzer, "");
@@ -912,9 +998,9 @@ public class TestQueryParser extends LocalizedTestCase {
       }
 
       @Override
      protected Query getFieldQuery(String field, String queryText) throws ParseException {
      protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
         type[0]=3;
        return super.getFieldQuery(field, queryText);
        return super.getFieldQuery(field, queryText, quoted);
       }
     };
 
diff --git a/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java b/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
index 3877ddd629c..abec28a27e7 100755
-- a/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
++ b/solr/src/java/org/apache/solr/search/ExtendedDismaxQParserPlugin.java
@@ -870,7 +870,7 @@ class ExtendedDismaxQParser extends QParser {
     int slop;
 
     @Override
    protected Query getFieldQuery(String field, String val) throws ParseException {
    protected Query getFieldQuery(String field, String val, boolean quoted) throws ParseException {
 //System.out.println("getFieldQuery: val="+val);
 
       this.type = QType.FIELD;
@@ -1005,7 +1005,7 @@ class ExtendedDismaxQParser extends QParser {
         switch (type) {
           case FIELD:  // fallthrough
           case PHRASE:
            Query query = super.getFieldQuery(field, val);
            Query query = super.getFieldQuery(field, val, type == QType.PHRASE);
             if (query instanceof PhraseQuery) {
               PhraseQuery pq = (PhraseQuery)query;
               if (minClauseSize > 1 && pq.getTerms().length < minClauseSize) return null;
diff --git a/solr/src/java/org/apache/solr/search/SolrQueryParser.java b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
index 1b4d9a763a0..18fba51d195 100644
-- a/solr/src/java/org/apache/solr/search/SolrQueryParser.java
++ b/solr/src/java/org/apache/solr/search/SolrQueryParser.java
@@ -128,7 +128,7 @@ public class SolrQueryParser extends QueryParser {
     }
   }
 
  protected Query getFieldQuery(String field, String queryText) throws ParseException {
  protected Query getFieldQuery(String field, String queryText, boolean quoted) throws ParseException {
     checkNullField(field);
     // intercept magic field name of "_" to use as a hook for our
     // own functions.
@@ -152,7 +152,7 @@ public class SolrQueryParser extends QueryParser {
     }
 
     // default to a normal field query
    return super.getFieldQuery(field, queryText);
    return super.getFieldQuery(field, queryText, quoted);
   }
 
   protected Query getRangeQuery(String field, String part1, String part2, boolean inclusive) throws ParseException {
diff --git a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
index ad658b7c1a7..ab7eb7c3299 100644
-- a/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
++ b/solr/src/java/org/apache/solr/util/SolrPluginUtils.java
@@ -776,7 +776,7 @@ public class SolrPluginUtils {
      * DisjunctionMaxQuery.  (so yes: aliases which point at other
      * aliases should work)
      */
    protected Query getFieldQuery(String field, String queryText)
    protected Query getFieldQuery(String field, String queryText, boolean quoted)
       throws ParseException {
             
       if (aliases.containsKey(field)) {
@@ -791,7 +791,7 @@ public class SolrPluginUtils {
                 
         for (String f : a.fields.keySet()) {
 
          Query sub = getFieldQuery(f,queryText);
          Query sub = getFieldQuery(f,queryText,quoted);
           if (null != sub) {
             if (null != a.fields.get(f)) {
               sub.setBoost(a.fields.get(f));
@@ -804,7 +804,7 @@ public class SolrPluginUtils {
 
       } else {
         try {
          return super.getFieldQuery(field, queryText);
          return super.getFieldQuery(field, queryText, quoted);
         } catch (Exception e) {
           return null;
         }
- 
2.19.1.windows.1

