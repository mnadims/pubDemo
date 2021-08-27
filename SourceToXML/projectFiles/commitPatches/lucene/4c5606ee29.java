From 4c5606ee29e767db929999d906a66e9db32d7c68 Mon Sep 17 00:00:00 2001
From: Christopher John Male <chrism@apache.org>
Date: Mon, 12 Sep 2011 05:50:26 +0000
Subject: [PATCH] LUCENE-3396: Converted most Analyzers over to using
 ReusableAnalyzerBase

git-svn-id: https://svn.apache.org/repos/asf/lucene/dev/trunk@1169607 13f79535-47bb-0310-9956-ffa450edef68
--
 lucene/CHANGES.txt                            |   9 +
 .../search/highlight/HighlighterTest.java     |   6 +-
 .../highlight/OffsetLimitTokenFilterTest.java |  14 +-
 .../search/highlight/TokenSourcesTest.java    |  12 +-
 .../vectorhighlight/AbstractTestCase.java     |  12 +-
 .../vectorhighlight/IndexTimeSynonymTest.java |  16 +-
 .../lucene/analysis/ReusableAnalyzerBase.java | 171 ++++++++++++++++--
 .../apache/lucene/analysis/MockAnalyzer.java  |  37 +---
 .../lucene/analysis/MockPayloadAnalyzer.java  |   9 +-
 .../org/apache/lucene/TestAssertions.java     |  37 ++--
 .../lucene/index/TestDocumentWriter.java      |  21 +--
 .../apache/lucene/index/TestIndexWriter.java  |  12 +-
 .../lucene/index/TestIndexWriterCommit.java   |  21 +--
 .../lucene/index/TestIndexWriterDelete.java   |  11 +-
 .../index/TestIndexWriterExceptions.java      |  32 ++--
 .../lucene/index/TestLazyProxSkipping.java    |  11 +-
 .../lucene/index/TestMultiLevelSkipList.java  |  12 +-
 .../org/apache/lucene/index/TestPayloads.java |  10 +-
 .../index/TestSameTokenSamePosition.java      |   9 +-
 .../lucene/index/TestTermVectorsReader.java   |   8 +-
 .../apache/lucene/index/TestTermdocPerf.java  |   8 +-
 .../lucene/search/TestMultiPhraseQuery.java   |   7 +-
 .../apache/lucene/search/TestPhraseQuery.java |   6 +-
 .../lucene/search/TestPositionIncrement.java  |   8 +-
 .../lucene/search/TestTermRangeQuery.java     |  17 +-
 .../lucene/search/payloads/PayloadHelper.java |  12 +-
 .../search/payloads/TestPayloadNearQuery.java |   9 +-
 .../search/payloads/TestPayloadTermQuery.java |  12 +-
 .../lucene/search/spans/TestBasics.java       |   7 +-
 .../lucene/search/spans/TestPayloadSpans.java |  27 +--
 modules/analysis/CHANGES.txt                  |   2 +
 .../miscellaneous/PatternAnalyzer.java        |  27 ++-
 .../analysis/standard/ClassicAnalyzer.java    |   4 +-
 .../analysis/standard/StandardAnalyzer.java   |   4 +-
 .../analysis/cn/TestChineseTokenizer.java     |  17 +-
 .../commongrams/CommonGramsFilterTest.java    |  25 ++-
 .../lucene/analysis/core/TestAnalyzers.java   |  15 +-
 .../miscellaneous/PatternAnalyzerTest.java    |   3 +-
 .../TestWordDelimiterFilter.java              |  28 ++-
 .../query/QueryAutoStopWordAnalyzerTest.java  |  26 ---
 .../shingle/ShingleAnalyzerWrapperTest.java   |  34 ----
 .../collation/TestCollationKeyFilter.java     |  13 +-
 .../analysis/icu/TestICUFoldingFilter.java    |  12 +-
 .../icu/TestICUNormalizer2Filter.java         |  23 ++-
 .../collation/TestICUCollationKeyFilter.java  |  13 +-
 .../cn/smart/SmartChineseAnalyzer.java        |  37 +---
 .../search/CategoryListIteratorTest.java      |  11 +-
 .../analyzing/TestAnalyzingQueryParser.java   |  16 +-
 .../classic/TestMultiAnalyzer.java            |  24 +--
 .../classic/TestMultiFieldQueryParser.java    |  15 +-
 .../classic/TestMultiPhraseQueryParsing.java  |   7 +-
 .../precedence/TestPrecedenceQueryParser.java |   7 +-
 .../standard/TestMultiAnalyzerQPHelper.java   |  24 +--
 .../standard/TestMultiFieldQPHelper.java      |  15 +-
 .../flexible/standard/TestQPHelper.java       |  19 +-
 .../queryparser/util/QueryParserTestBase.java |  47 +++--
 56 files changed, 494 insertions(+), 557 deletions(-)

diff --git a/lucene/CHANGES.txt b/lucene/CHANGES.txt
index abd9e84054c..58ce5869f2a 100644
-- a/lucene/CHANGES.txt
++ b/lucene/CHANGES.txt
@@ -171,6 +171,9 @@ Changes in backwards compatibility policy
   IndexableFieldType.  See MIGRATE.txt for more details.
   (Nikola Tankovic, Mike McCandless, Chris Male)
 
* LUCENE-3396: ReusableAnalyzerBase.TokenStreamComponents.reset(Reader) now returns void instead
  of boolean.  If a Component cannot be reset, it should throw an Exception. 

 Changes in Runtime Behavior
 
 * LUCENE-2846: omitNorms now behaves like omitTermFrequencyAndPositions, if you
@@ -523,6 +526,12 @@ New features
 
   (David Mark Nemeskey via Robert Muir)
 
* LUCENE-3396: ReusableAnalyzerBase now provides a ReuseStrategy abstraction which
  controls how TokenStreamComponents are reused per request.  Two implementations are
  provided - GlobalReuseStrategy which implements the current behavior of sharing
  components between all fields, and PerFieldReuseStrategy which shares per field.
  (Chris Male)

 Optimizations
 
 * LUCENE-2588: Don't store unnecessary suffixes when writing the terms
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
index 728edf5c5c7..2b2e176942b 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/HighlighterTest.java
@@ -1802,7 +1802,7 @@ public class HighlighterTest extends BaseTokenStreamTestCase implements Formatte
 // behaviour to synonyms
 // ===================================================================
 
final class SynonymAnalyzer extends Analyzer {
final class SynonymAnalyzer extends ReusableAnalyzerBase {
   private Map<String,String> synonyms;
 
   public SynonymAnalyzer(Map<String,String> synonyms) {
@@ -1816,12 +1816,12 @@ final class SynonymAnalyzer extends Analyzer {
    *      java.io.Reader)
    */
   @Override
  public TokenStream tokenStream(String arg0, Reader arg1) {
  public TokenStreamComponents createComponents(String arg0, Reader arg1) {
     Tokenizer stream = new MockTokenizer(arg1, MockTokenizer.SIMPLE, true);
     stream.addAttribute(CharTermAttribute.class);
     stream.addAttribute(PositionIncrementAttribute.class);
     stream.addAttribute(OffsetAttribute.class);
    return new SynonymTokenizer(stream, synonyms);
    return new TokenStreamComponents(stream, new SynonymTokenizer(stream, synonyms));
   }
 }
 
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
index 30dccc4bcc8..8afcecd6f00 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/OffsetLimitTokenFilterTest.java
@@ -20,10 +20,7 @@ package org.apache.lucene.search.highlight;
 import java.io.Reader;
 import java.io.StringReader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 
 public class OffsetLimitTokenFilterTest extends BaseTokenStreamTestCase {
   
@@ -52,15 +49,14 @@ public class OffsetLimitTokenFilterTest extends BaseTokenStreamTestCase {
     assertTokenStreamContents(filter, new String[] {"short", "toolong",
         "evenmuchlongertext"});
     
    // TODO: This is not actually testing reuse! (reusableTokenStream is not implemented)
    checkOneTermReuse(new Analyzer() {
    checkOneTermReuse(new ReusableAnalyzerBase() {
       
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         tokenizer.setEnableChecks(false);
        return new OffsetLimitTokenFilter(tokenizer, 10);
        return new TokenStreamComponents(tokenizer, new OffsetLimitTokenFilter(tokenizer, 10));
       }
     }, "llenges", "llenges");
   }
}
\ No newline at end of file
}
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
index c368128c7a9..0e0bb8585ab 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/highlight/TokenSourcesTest.java
@@ -20,9 +20,7 @@ package org.apache.lucene.search.highlight;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -50,15 +48,15 @@ import org.apache.lucene.util.LuceneTestCase;
 public class TokenSourcesTest extends LuceneTestCase {
   private static final String FIELD = "text";
 
  private static final class OverlapAnalyzer extends Analyzer {
  private static final class OverlapAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new TokenStreamOverlap();
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new TokenStreamOverlap());
     }
   }
 
  private static final class TokenStreamOverlap extends TokenStream {
  private static final class TokenStreamOverlap extends Tokenizer {
     private Token[] tokens;
 
     private int i = -1;
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
index befaef311e0..c28b3dac6c0 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/AbstractTestCase.java
@@ -24,11 +24,7 @@ import java.util.ArrayList;
 import java.util.Collection;
 import java.util.List;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
@@ -198,10 +194,10 @@ public abstract class AbstractTestCase extends LuceneTestCase {
     return phraseQuery;
   }
 
  static final class BigramAnalyzer extends Analyzer {
  static final class BigramAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new BasicNGramTokenizer( reader );
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new BasicNGramTokenizer(reader));
     }
   }
   
diff --git a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
index abe550ddee0..433c6347bb2 100644
-- a/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
++ b/lucene/contrib/highlighter/src/test/org/apache/lucene/search/vectorhighlight/IndexTimeSynonymTest.java
@@ -22,9 +22,7 @@ import java.io.Reader;
 import java.util.HashSet;
 import java.util.Set;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.search.BooleanQuery;
 import org.apache.lucene.search.BooleanClause.Occur;
@@ -292,15 +290,15 @@ public class IndexTimeSynonymTest extends AbstractTestCase {
     return token;
   }
   
  public static final class TokenArrayAnalyzer extends Analyzer {
    Token[] tokens;
    public TokenArrayAnalyzer( Token... tokens ){
  public static final class TokenArrayAnalyzer extends ReusableAnalyzerBase {
    final Token[] tokens;
    public TokenArrayAnalyzer(Token... tokens) {
       this.tokens = tokens;
     }
     
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {      
      TokenStream ts = new TokenStream(Token.TOKEN_ATTRIBUTE_FACTORY) {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer ts = new Tokenizer(Token.TOKEN_ATTRIBUTE_FACTORY) {
         final AttributeImpl reusableToken = (AttributeImpl) addAttribute(CharTermAttribute.class);
         int p = 0;
         
@@ -318,7 +316,7 @@ public class IndexTimeSynonymTest extends AbstractTestCase {
           this.p = 0;
         }
       };
      return ts;
      return new TokenStreamComponents(ts);
     }
   }
 }
diff --git a/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java b/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
index baf8f4ccbc4..638e7ab53d3 100644
-- a/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
++ b/lucene/src/java/org/apache/lucene/analysis/ReusableAnalyzerBase.java
@@ -17,8 +17,13 @@ package org.apache.lucene.analysis;
  * limitations under the License.
  */
 
import org.apache.lucene.store.AlreadyClosedException;
import org.apache.lucene.util.CloseableThreadLocal;

 import java.io.IOException;
 import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
 
 /**
  * An convenience subclass of Analyzer that makes it easy to implement
@@ -38,6 +43,16 @@ import java.io.Reader;
  */
 public abstract class ReusableAnalyzerBase extends Analyzer {
 
  private final ReuseStrategy reuseStrategy;

  public ReusableAnalyzerBase() {
    this(new GlobalReuseStrategy());
  }

  public ReusableAnalyzerBase(ReuseStrategy reuseStrategy) {
    this.reuseStrategy = reuseStrategy;
  }

   /**
    * Creates a new {@link TokenStreamComponents} instance for this analyzer.
    * 
@@ -66,14 +81,15 @@ public abstract class ReusableAnalyzerBase extends Analyzer {
   @Override
   public final TokenStream reusableTokenStream(final String fieldName,
       final Reader reader) throws IOException {
    TokenStreamComponents streamChain = (TokenStreamComponents)
    getPreviousTokenStream();
    TokenStreamComponents components = reuseStrategy.getReusableComponents(fieldName);
     final Reader r = initReader(reader);
    if (streamChain == null || !streamChain.reset(r)) {
      streamChain = createComponents(fieldName, r);
      setPreviousTokenStream(streamChain);
    if (components == null) {
      components = createComponents(fieldName, r);
      reuseStrategy.setReusableComponents(fieldName, components);
    } else {
      components.reset(r);
     }
    return streamChain.getTokenStream();
    return components.getTokenStream();
   }
 
   /**
@@ -98,7 +114,16 @@ public abstract class ReusableAnalyzerBase extends Analyzer {
   protected Reader initReader(Reader reader) {
     return reader;
   }
  

  /**
   * {@inheritDoc}
   */
  @Override
  public void close() {
    super.close();
    reuseStrategy.close();
  }

   /**
    * This class encapsulates the outer components of a token stream. It provides
    * access to the source ({@link Tokenizer}) and the outer end (sink), an
@@ -137,22 +162,16 @@ public abstract class ReusableAnalyzerBase extends Analyzer {
     }
 
     /**
     * Resets the encapsulated components with the given reader. This method by
     * default returns <code>true</code> indicating that the components have
     * been reset successfully. Subclasses of {@link ReusableAnalyzerBase} might use
     * their own {@link TokenStreamComponents} returning <code>false</code> if
     * the components cannot be reset.
     * Resets the encapsulated components with the given reader. If the components
     * cannot be reset, an Exception should be thrown.
      * 
      * @param reader
      *          a reader to reset the source component
     * @return <code>true</code> if the components were reset, otherwise
     *         <code>false</code>
      * @throws IOException
      *           if the component's reset method throws an {@link IOException}
      */
    protected boolean reset(final Reader reader) throws IOException {
    protected void reset(final Reader reader) throws IOException {
       source.reset(reader);
      return true;
     }
 
     /**
@@ -166,4 +185,124 @@ public abstract class ReusableAnalyzerBase extends Analyzer {
 
   }
 
  /**
   * Strategy defining how TokenStreamComponents are reused per call to
   * {@link ReusableAnalyzerBase#tokenStream(String, java.io.Reader)}.
   */
  public static abstract class ReuseStrategy {

    private CloseableThreadLocal<Object> storedValue = new CloseableThreadLocal<Object>();

    /**
     * Gets the reusable TokenStreamComponents for the field with the given name
     *
     * @param fieldName Name of the field whose reusable TokenStreamComponents
     *        are to be retrieved
     * @return Reusable TokenStreamComponents for the field, or {@code null}
     *         if there was no previous components for the field
     */
    public abstract TokenStreamComponents getReusableComponents(String fieldName);

    /**
     * Stores the given TokenStreamComponents as the reusable components for the
     * field with the give name
     *
     * @param fieldName Name of the field whose TokenStreamComponents are being set
     * @param components TokenStreamComponents which are to be reused for the field
     */
    public abstract void setReusableComponents(String fieldName, TokenStreamComponents components);

    /**
     * Returns the currently stored value
     *
     * @return Currently stored value or {@code null} if no value is stored
     */
    protected final Object getStoredValue() {
      try {
        return storedValue.get();
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Sets the stored value
     *
     * @param storedValue Value to store
     */
    protected final void setStoredValue(Object storedValue) {
      try {
        this.storedValue.set(storedValue);
      } catch (NullPointerException npe) {
        if (storedValue == null) {
          throw new AlreadyClosedException("this Analyzer is closed");
        } else {
          throw npe;
        }
      }
    }

    /**
     * Closes the ReuseStrategy, freeing any resources
     */
    public void close() {
      storedValue.close();
      storedValue = null;
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses the same components for
   * every field.
   */
  public final static class GlobalReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    public TokenStreamComponents getReusableComponents(String fieldName) {
      return (TokenStreamComponents) getStoredValue();
    }

    /**
     * {@inheritDoc}
     */
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      setStoredValue(components);
    }
  }

  /**
   * Implementation of {@link ReuseStrategy} that reuses components per-field by
   * maintaining a Map of TokenStreamComponent per field name.
   */
  public static class PerFieldReuseStrategy extends ReuseStrategy {

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public TokenStreamComponents getReusableComponents(String fieldName) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      return componentsPerField != null ? componentsPerField.get(fieldName) : null;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public void setReusableComponents(String fieldName, TokenStreamComponents components) {
      Map<String, TokenStreamComponents> componentsPerField = (Map<String, TokenStreamComponents>) getStoredValue();
      if (componentsPerField == null) {
        componentsPerField = new HashMap<String, TokenStreamComponents>();
        setStoredValue(componentsPerField);
      }
      componentsPerField.put(fieldName, components);
    }
  }

 }
diff --git a/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java b/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
index dbd28fe67fc..6762bd03b89 100644
-- a/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
++ b/lucene/src/test-framework/org/apache/lucene/analysis/MockAnalyzer.java
@@ -42,7 +42,7 @@ import org.apache.lucene.util.automaton.CharacterRunAutomaton;
  * </ul>
  * @see MockTokenizer
  */
public final class MockAnalyzer extends Analyzer { 
public final class MockAnalyzer extends ReusableAnalyzerBase {
   private final CharacterRunAutomaton runAutomaton;
   private final boolean lowerCase;
   private final CharacterRunAutomaton filter;
@@ -62,6 +62,7 @@ public final class MockAnalyzer extends Analyzer {
    * @param enablePositionIncrements true if position increments should reflect filtered terms.
    */
   public MockAnalyzer(Random random, CharacterRunAutomaton runAutomaton, boolean lowerCase, CharacterRunAutomaton filter, boolean enablePositionIncrements) {
    super(new PerFieldReuseStrategy());
     this.random = random;
     this.runAutomaton = runAutomaton;
     this.lowerCase = lowerCase;
@@ -88,41 +89,11 @@ public final class MockAnalyzer extends Analyzer {
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
  public TokenStreamComponents createComponents(String fieldName, Reader reader) {
     MockTokenizer tokenizer = new MockTokenizer(reader, runAutomaton, lowerCase);
     tokenizer.setEnableChecks(enableChecks);
     TokenFilter filt = new MockTokenFilter(tokenizer, filter, enablePositionIncrements);
    filt = maybePayload(filt, fieldName);
    return filt;
  }

  private class SavedStreams {
    MockTokenizer tokenizer;
    TokenFilter filter;
  }

  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader)
      throws IOException {
    @SuppressWarnings("unchecked") Map<String,SavedStreams> map = (Map) getPreviousTokenStream();
    if (map == null) {
      map = new HashMap<String,SavedStreams>();
      setPreviousTokenStream(map);
    }
    
    SavedStreams saved = map.get(fieldName);
    if (saved == null) {
      saved = new SavedStreams();
      saved.tokenizer = new MockTokenizer(reader, runAutomaton, lowerCase);
      saved.tokenizer.setEnableChecks(enableChecks);
      saved.filter = new MockTokenFilter(saved.tokenizer, filter, enablePositionIncrements);
      saved.filter = maybePayload(saved.filter, fieldName);
      map.put(fieldName, saved);
      return saved.filter;
    } else {
      saved.tokenizer.reset(reader);
      return saved.filter;
    }
    return new TokenStreamComponents(tokenizer, maybePayload(filt, fieldName));
   }
   
   private synchronized TokenFilter maybePayload(TokenFilter stream, String fieldName) {
diff --git a/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java b/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
index fe64ad8884e..dbf9c2a2026 100644
-- a/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
++ b/lucene/src/test-framework/org/apache/lucene/analysis/MockPayloadAnalyzer.java
@@ -30,16 +30,15 @@ import java.io.Reader;
  *
  *
  **/
public final class MockPayloadAnalyzer extends Analyzer {
public final class MockPayloadAnalyzer extends ReusableAnalyzerBase {
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
    return new MockPayloadFilter(result, fieldName);
  public TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
    return new TokenStreamComponents(result, new MockPayloadFilter(result, fieldName));
   }
 }
 

 /**
  *
  *
diff --git a/lucene/src/test/org/apache/lucene/TestAssertions.java b/lucene/src/test/org/apache/lucene/TestAssertions.java
index ce51fd34484..4a3c75e1310 100644
-- a/lucene/src/test/org/apache/lucene/TestAssertions.java
++ b/lucene/src/test/org/apache/lucene/TestAssertions.java
@@ -19,6 +19,7 @@ package org.apache.lucene;
 
 import java.io.Reader;
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.util.LuceneTestCase;
 import org.apache.lucene.analysis.Analyzer;
 import org.apache.lucene.analysis.TokenStream;
@@ -34,32 +35,36 @@ public class TestAssertions extends LuceneTestCase {
     }
   }
   
  static class TestAnalyzer1 extends Analyzer {
    @Override
    public final TokenStream tokenStream(String s, Reader r) { return null; }
  static class TestAnalyzer1 extends ReusableAnalyzerBase {

     @Override
    public final TokenStream reusableTokenStream(String s, Reader r) { return null; }
    protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
      return null;
    }
   }
 
  static final class TestAnalyzer2 extends Analyzer {
    @Override
    public TokenStream tokenStream(String s, Reader r) { return null; }
  static final class TestAnalyzer2 extends ReusableAnalyzerBase {

     @Override
    public TokenStream reusableTokenStream(String s, Reader r) { return null; }
    protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
      return null;
    }
   }
 
  static class TestAnalyzer3 extends Analyzer {
    @Override
    public TokenStream tokenStream(String s, Reader r) { return null; }
  static class TestAnalyzer3 extends ReusableAnalyzerBase {

     @Override
    public TokenStream reusableTokenStream(String s, Reader r) { return null; }
    protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
      return null;
    }
   }
 
  static class TestAnalyzer4 extends Analyzer {
    @Override
    public final TokenStream tokenStream(String s, Reader r) { return null; }
  static class TestAnalyzer4 extends ReusableAnalyzerBase {

     @Override
    public TokenStream reusableTokenStream(String s, Reader r) { return null; }
    protected TokenStreamComponents createComponents(String fieldName, Reader aReader) {
      return null;
    }
   }
 
   static class TestTokenStream1 extends TokenStream {
diff --git a/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java b/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
index 45d92667c8d..3dc7c055a9b 100644
-- a/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestDocumentWriter.java
@@ -20,11 +20,7 @@ package org.apache.lucene.index;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -107,10 +103,10 @@ public class TestDocumentWriter extends LuceneTestCase {
   }
 
   public void testPositionIncrementGap() throws IOException {
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
       }
 
       @Override
@@ -142,10 +138,11 @@ public class TestDocumentWriter extends LuceneTestCase {
   }
 
   public void testTokenReuse() throws IOException {
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new TokenFilter(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false)) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new TokenFilter(tokenizer) {
           boolean first = true;
           AttributeSource.State state;
 
@@ -187,7 +184,7 @@ public class TestDocumentWriter extends LuceneTestCase {
           final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
           final PayloadAttribute payloadAtt = addAttribute(PayloadAttribute.class);
           final PositionIncrementAttribute posIncrAtt = addAttribute(PositionIncrementAttribute.class);
        };
        });
       }
     };
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
index 6fdb72bda79..9fca64934d8 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriter.java
@@ -31,11 +31,7 @@ import java.util.List;
 import java.util.Map;
 import java.util.Random;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.document.BinaryField;
@@ -1710,10 +1706,10 @@ public class TestIndexWriter extends LuceneTestCase {
     dir.close();
   }
 
  static final class StringSplitAnalyzer extends Analyzer {
  static final class StringSplitAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new StringSplitTokenizer(reader);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new StringSplitTokenizer(reader));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
index 9bde4a3eb38..553cf076b91 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterCommit.java
@@ -23,11 +23,7 @@ import java.util.HashMap;
 import java.util.Map;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockFixedLengthPayloadFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.StringField;
@@ -179,21 +175,20 @@ public class TestIndexWriterCommit extends LuceneTestCase {
     Analyzer analyzer;
     if (random.nextBoolean()) {
       // no payloads
     analyzer = new Analyzer() {
     analyzer = new ReusableAnalyzerBase() {
         @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
          return new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
        public TokenStreamComponents createComponents(String fieldName, Reader reader) {
          return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
         }
       };
     } else {
       // fixed length payloads
       final int length = random.nextInt(200);
      analyzer = new Analyzer() {
      analyzer = new ReusableAnalyzerBase() {
         @Override
        public TokenStream tokenStream(String fieldName, Reader reader) {
          return new MockFixedLengthPayloadFilter(random,
              new MockTokenizer(reader, MockTokenizer.WHITESPACE, true),
              length);
        public TokenStreamComponents createComponents(String fieldName, Reader reader) {
          Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
          return new TokenStreamComponents(tokenizer, new MockFixedLengthPayloadFilter(random, tokenizer, length));
         }
       };
     }
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
index bc35eb4adfb..bfea6ddcea3 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterDelete.java
@@ -26,10 +26,7 @@ import java.util.Random;
 import java.util.concurrent.atomic.AtomicInteger;
 import java.util.concurrent.atomic.AtomicBoolean;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.FieldType;
 import org.apache.lucene.document.StringField;
@@ -902,10 +899,10 @@ public class TestIndexWriterDelete extends LuceneTestCase {
     final Random r = random;
     Directory dir = newDirectory();
     // note this test explicitly disables payloads
    final Analyzer analyzer = new Analyzer() {
    final Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
       }
     };
     IndexWriter w = new IndexWriter(dir, newIndexWriterConfig( TEST_VERSION_CURRENT, analyzer).setRAMBufferSizeMB(1.0).setMaxBufferedDocs(IndexWriterConfig.DISABLE_AUTO_FLUSH).setMaxBufferedDeleteTerms(IndexWriterConfig.DISABLE_AUTO_FLUSH));
diff --git a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
index 74ac08fca36..d0369b0bce4 100644
-- a/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
++ b/lucene/src/test/org/apache/lucene/index/TestIndexWriterExceptions.java
@@ -27,11 +27,7 @@ import java.util.Iterator;
 import java.util.List;
 import java.util.Random;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.Field;
 import org.apache.lucene.document.FieldType;
@@ -390,12 +386,12 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
     doc.add(newField("field", "a field", TextField.TYPE_STORED));
     w.addDocument(doc);
 
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         tokenizer.setEnableChecks(false); // disable workflow checking as we forcefully close() in exceptional cases.
        return new CrashingFilter(fieldName, tokenizer);
        return new TokenStreamComponents(tokenizer, new CrashingFilter(fieldName, tokenizer));
       }
     };
 
@@ -458,13 +454,13 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   // LUCENE-1072
   public void testExceptionFromTokenStream() throws IOException {
     Directory dir = newDirectory();
    IndexWriterConfig conf = newIndexWriterConfig( TEST_VERSION_CURRENT, new Analyzer() {
    IndexWriterConfig conf = newIndexWriterConfig( TEST_VERSION_CURRENT, new ReusableAnalyzerBase() {
 
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
         tokenizer.setEnableChecks(false); // disable workflow checking as we forcefully close() in exceptional cases.
        return new TokenFilter(tokenizer) {
        return new TokenStreamComponents(tokenizer, new TokenFilter(tokenizer) {
           private int count = 0;
 
           @Override
@@ -480,7 +476,7 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
             super.reset();
             this.count = 0;
           }
        };
        });
       }
 
     });
@@ -595,12 +591,12 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   }
 
   public void testDocumentsWriterExceptions() throws IOException {
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         tokenizer.setEnableChecks(false); // disable workflow checking as we forcefully close() in exceptional cases.
        return new CrashingFilter(fieldName, tokenizer);
        return new TokenStreamComponents(tokenizer, new CrashingFilter(fieldName, tokenizer));
       }
     };
 
@@ -691,12 +687,12 @@ public class TestIndexWriterExceptions extends LuceneTestCase {
   }
 
   public void testDocumentsWriterExceptionThreads() throws Exception {
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase(new ReusableAnalyzerBase.PerFieldReuseStrategy()) {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
         MockTokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         tokenizer.setEnableChecks(false); // disable workflow checking as we forcefully close() in exceptional cases.
        return new CrashingFilter(fieldName, tokenizer);
        return new TokenStreamComponents(tokenizer, new CrashingFilter(fieldName, tokenizer));
       }
     };
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java b/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
index 95bf55a37a2..d5975eb2bea 100755
-- a/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
++ b/lucene/src/test/org/apache/lucene/index/TestLazyProxSkipping.java
@@ -20,10 +20,7 @@ package org.apache.lucene.index;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.codecs.CodecProvider;
@@ -71,10 +68,10 @@ public class TestLazyProxSkipping extends LuceneTestCase {
     private void createIndex(int numHits) throws IOException {
         int numDocs = 500;
         
        final Analyzer analyzer = new Analyzer() {
        final Analyzer analyzer = new ReusableAnalyzerBase() {
           @Override
          public TokenStream tokenStream(String fieldName, Reader reader) {
            return new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
          public TokenStreamComponents createComponents(String fieldName, Reader reader) {
            return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
           }
         };
         Directory directory = new SeekCountingDirectory(new RAMDirectory());
diff --git a/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java b/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
index 599d490051a..6f4a5d97254 100644
-- a/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
++ b/lucene/src/test/org/apache/lucene/index/TestMultiLevelSkipList.java
@@ -21,10 +21,7 @@ import java.io.IOException;
 import java.io.Reader;
 import java.util.concurrent.atomic.AtomicInteger;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.TextField;
@@ -114,11 +111,12 @@ public class TestMultiLevelSkipList extends LuceneTestCase {
     assertEquals("Wrong payload for the target " + target + ": " + b.bytes[b.offset], (byte) target, b.bytes[b.offset]);
   }
 
  private static class PayloadAnalyzer extends Analyzer {
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
     private final AtomicInteger payloadCount = new AtomicInteger(-1);
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new PayloadFilter(payloadCount, new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(tokenizer, new PayloadFilter(payloadCount, tokenizer));
     }
 
   }
diff --git a/lucene/src/test/org/apache/lucene/index/TestPayloads.java b/lucene/src/test/org/apache/lucene/index/TestPayloads.java
index 6ece3e5d9e8..c1ba6cb8dea 100644
-- a/lucene/src/test/org/apache/lucene/index/TestPayloads.java
++ b/lucene/src/test/org/apache/lucene/index/TestPayloads.java
@@ -25,11 +25,7 @@ import java.util.HashMap;
 import java.util.List;
 import java.util.Map;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
@@ -105,12 +101,12 @@ public class TestPayloads extends LuceneTestCase {
         // so this field is used to check if the DocumentWriter correctly enables the payloads bit
         // even if only some term positions have payloads
         d.add(newField("f2", "This field has payloads in all docs", TextField.TYPE_UNSTORED));
        d.add(newField("f2", "This field has payloads in all docs", TextField.TYPE_UNSTORED));
        d.add(newField("f2", "This field has payloads in all docs NO PAYLOAD", TextField.TYPE_UNSTORED));
         // this field is used to verify if the SegmentMerger enables payloads for a field if it has payloads 
         // enabled in only some documents
         d.add(newField("f3", "This field has payloads in some docs", TextField.TYPE_UNSTORED));
         // only add payload data for field f2
        analyzer.setPayloadData("f2", 1, "somedata".getBytes(), 0, 1);
        analyzer.setPayloadData("f2", "somedata".getBytes(), 0, 1);
         writer.addDocument(d);
         // flush
         writer.close();
diff --git a/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java b/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
index 8e9e58c92ec..b117adab69b 100644
-- a/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
++ b/lucene/src/test/org/apache/lucene/index/TestSameTokenSamePosition.java
@@ -20,8 +20,7 @@ package org.apache.lucene.index;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
@@ -64,10 +63,10 @@ public class TestSameTokenSamePosition extends LuceneTestCase {
   }
 }
 
final class BugReproAnalyzer extends Analyzer{
final class BugReproAnalyzer extends ReusableAnalyzerBase {
   @Override
  public TokenStream tokenStream(String arg0, Reader arg1) {
    return new BugReproAnalyzerTokenizer();
  public TokenStreamComponents createComponents(String arg0, Reader arg1) {
    return new TokenStreamComponents(new BugReproAnalyzerTokenizer());
   }
 }
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java b/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
index 6a93c435731..2aad70c16f9 100644
-- a/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
++ b/lucene/src/test/org/apache/lucene/index/TestTermVectorsReader.java
@@ -137,7 +137,7 @@ public class TestTermVectorsReader extends LuceneTestCase {
     super.tearDown();
   }
 
  private class MyTokenStream extends TokenStream {
  private class MyTokenStream extends Tokenizer {
     private int tokenUpto;
     
     private final CharTermAttribute termAtt;
@@ -175,10 +175,10 @@ public class TestTermVectorsReader extends LuceneTestCase {
     }
   }
 
  private class MyAnalyzer extends Analyzer {
  private class MyAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new MyTokenStream();
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new MyTokenStream());
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java b/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
index f5c3dcdd89e..dbe1b94fbd8 100644
-- a/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
++ b/lucene/src/test/org/apache/lucene/index/TestTermdocPerf.java
@@ -22,7 +22,7 @@ import java.io.Reader;
 import java.util.Random;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.document.Document;
@@ -77,10 +77,10 @@ public class TestTermdocPerf extends LuceneTestCase {
   void addDocs(final Random random, Directory dir, final int ndocs, String field, final String val, final int maxTF, final float percentDocs) throws IOException {
     final RepeatingTokenStream ts = new RepeatingTokenStream(val, random, percentDocs, maxTF);
 
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return ts;
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(ts);
       }
     };
 
diff --git a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
index 5ef3a09b701..d63296c53ef 100644
-- a/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestMultiPhraseQuery.java
@@ -17,6 +17,7 @@ package org.apache.lucene.search;
  * limitations under the License.
  */
 
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.index.IndexWriterConfig;
 import org.apache.lucene.index.RandomIndexWriter;
 import org.apache.lucene.index.Term;
@@ -345,7 +346,7 @@ public class TestMultiPhraseQuery extends LuceneTestCase {
     }
   }
 
  private static class CannedAnalyzer extends Analyzer {
  private static class CannedAnalyzer extends ReusableAnalyzerBase {
     private final TokenAndPos[] tokens;
     
     public CannedAnalyzer(TokenAndPos[] tokens) {
@@ -353,8 +354,8 @@ public class TestMultiPhraseQuery extends LuceneTestCase {
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new CannedTokenizer(tokens);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new CannedTokenizer(tokens));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
index 3cb9a282b76..a60a8824377 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestPhraseQuery.java
@@ -55,10 +55,10 @@ public class TestPhraseQuery extends LuceneTestCase {
   @BeforeClass
   public static void beforeClass() throws Exception {
     directory = newDirectory();
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
       }
 
       @Override
diff --git a/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java b/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
index 3cbe6610f46..4e59e0f3d43 100644
-- a/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
++ b/lucene/src/test/org/apache/lucene/search/TestPositionIncrement.java
@@ -56,10 +56,10 @@ public class TestPositionIncrement extends LuceneTestCase {
   final static boolean VERBOSE = false;
 
   public void testSetPosition() throws Exception {
    Analyzer analyzer = new Analyzer() {
    Analyzer analyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new TokenStream() {
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new Tokenizer() {
           private final String[] TOKENS = {"1", "2", "3", "4", "5"};
           private final int[] INCREMENTS = {0, 2, 1, 0, 1};
           private int i = 0;
@@ -85,7 +85,7 @@ public class TestPositionIncrement extends LuceneTestCase {
             super.reset();
             this.i = 0;
           }
        };
        });
       }
     };
     Directory store = newDirectory();
diff --git a/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java b/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
index 987fadc0564..3b6fa24d10c 100644
-- a/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
++ b/lucene/src/test/org/apache/lucene/search/TestTermRangeQuery.java
@@ -190,7 +190,7 @@ public class TestTermRangeQuery extends LuceneTestCase {
     assertFalse("queries with different inclusive are not equal", query.equals(other));
   }
 
  private static class SingleCharAnalyzer extends Analyzer {
  private static class SingleCharAnalyzer extends ReusableAnalyzerBase {
 
     private static class SingleCharTokenizer extends Tokenizer {
       char[] buffer = new char[1];
@@ -225,19 +225,8 @@ public class TestTermRangeQuery extends LuceneTestCase {
     }
 
     @Override
    public TokenStream reusableTokenStream(String fieldName, Reader reader) throws IOException {
      Tokenizer tokenizer = (Tokenizer) getPreviousTokenStream();
      if (tokenizer == null) {
        tokenizer = new SingleCharTokenizer(reader);
        setPreviousTokenStream(tokenizer);
      } else
        tokenizer.reset(reader);
      return tokenizer;
    }

    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SingleCharTokenizer(reader);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new SingleCharTokenizer(reader));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java b/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
index d799ca65df2..4648af7871f 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/PayloadHelper.java
@@ -55,14 +55,16 @@ public class PayloadHelper {
 
   public IndexReader reader;
 
  public final class PayloadAnalyzer extends Analyzer {
  public final class PayloadAnalyzer extends ReusableAnalyzerBase {
 
    public PayloadAnalyzer() {
      super(new PerFieldReuseStrategy());
    }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      result = new PayloadFilter(result, fieldName);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(result, new PayloadFilter(result, fieldName));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
index 0b216bcfd87..6b37b5c7d9b 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadNearQuery.java
@@ -55,12 +55,11 @@ public class TestPayloadNearQuery extends LuceneTestCase {
   private static byte[] payload2 = new byte[]{2};
   private static byte[] payload4 = new byte[]{4};
 
  private static class PayloadAnalyzer extends Analyzer {
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      result = new PayloadFilter(result, fieldName);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(result, new PayloadFilter(result, fieldName));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
index 8a102eda658..cd952447ebb 100644
-- a/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
++ b/lucene/src/test/org/apache/lucene/search/payloads/TestPayloadTermQuery.java
@@ -64,14 +64,16 @@ public class TestPayloadTermQuery extends LuceneTestCase {
   private static final byte[] payloadMultiField2 = new byte[]{4};
   protected static Directory directory;
 
  private static class PayloadAnalyzer extends Analyzer {
  private static class PayloadAnalyzer extends ReusableAnalyzerBase {
 
    private PayloadAnalyzer() {
      super(new PerFieldReuseStrategy());
    }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      result = new PayloadFilter(result, fieldName);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(result, new PayloadFilter(result, fieldName));
     }
   }
 
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java b/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
index 81e90e32b0e..c3a30c51477 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestBasics.java
@@ -25,6 +25,7 @@ import java.util.Collections;
 import java.util.List;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
 import org.apache.lucene.analysis.MockTokenizer;
 import org.apache.lucene.analysis.TokenFilter;
 import org.apache.lucene.analysis.TokenStream;
@@ -70,14 +71,12 @@ public class TestBasics extends LuceneTestCase {
   private static Directory directory;
 
   static final class SimplePayloadFilter extends TokenFilter {
    String fieldName;
     int pos;
     final PayloadAttribute payloadAttr;
     final CharTermAttribute termAttr;
 
    public SimplePayloadFilter(TokenStream input, String fieldName) {
    public SimplePayloadFilter(TokenStream input) {
       super(input);
      this.fieldName = fieldName;
       pos = 0;
       payloadAttr = input.addAttribute(PayloadAttribute.class);
       termAttr = input.addAttribute(CharTermAttribute.class);
@@ -105,7 +104,7 @@ public class TestBasics extends LuceneTestCase {
 
     @Override
     public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimplePayloadFilter(new MockTokenizer(reader, MockTokenizer.SIMPLE, true), fieldName);
      return new SimplePayloadFilter(new MockTokenizer(reader, MockTokenizer.SIMPLE, true));
     }
     
   };
diff --git a/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java b/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
index ed6d5b7b02d..85771161ef0 100644
-- a/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
++ b/lucene/src/test/org/apache/lucene/search/spans/TestPayloadSpans.java
@@ -23,10 +23,7 @@ import java.util.Collection;
 import java.util.HashSet;
 import java.util.Set;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
@@ -479,18 +476,16 @@ public class TestPayloadSpans extends LuceneTestCase {
     assertEquals(numSpans, cnt);
   }
 
  final class PayloadAnalyzer extends Analyzer {
  final class PayloadAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      result = new PayloadFilter(result, fieldName);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(result, new PayloadFilter(result));
     }
   }
 
   final class PayloadFilter extends TokenFilter {
    String fieldName;
     Set<String> entities = new HashSet<String>();
     Set<String> nopayload = new HashSet<String>();
     int pos;
@@ -498,9 +493,8 @@ public class TestPayloadSpans extends LuceneTestCase {
     CharTermAttribute termAtt;
     PositionIncrementAttribute posIncrAtt;
 
    public PayloadFilter(TokenStream input, String fieldName) {
    public PayloadFilter(TokenStream input) {
       super(input);
      this.fieldName = fieldName;
       pos = 0;
       entities.add("xx");
       entities.add("one");
@@ -536,13 +530,12 @@ public class TestPayloadSpans extends LuceneTestCase {
     }
   }
   
  public final class TestPayloadAnalyzer extends Analyzer {
  public final class TestPayloadAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      result = new PayloadFilter(result, fieldName);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(result, new PayloadFilter(result));
     }
   }
 }
diff --git a/modules/analysis/CHANGES.txt b/modules/analysis/CHANGES.txt
index 32ab0f10c51..58e9f0ca99e 100644
-- a/modules/analysis/CHANGES.txt
++ b/modules/analysis/CHANGES.txt
@@ -106,3 +106,5 @@ Build
  * LUCENE-2413: All analyzers in contrib/analyzers and contrib/icu were moved to the 
    analysis module.  The 'smartcn' and 'stempel' components now depend on 'common'.  
    (Robert Muir)

 * LUCENE-3376: Moved ReusableAnalyzerBase into lucene core. (Chris Male)
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
index 1d543551160..4c12b31afb6 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/miscellaneous/PatternAnalyzer.java
@@ -27,6 +27,7 @@ import java.util.regex.Matcher;
 import java.util.regex.Pattern;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.core.StopAnalyzer;
@@ -66,7 +67,7 @@ import org.apache.lucene.util.Version;
  * @deprecated (4.0) use the pattern-based analysis in the analysis/pattern package instead.
  */
 @Deprecated
public final class PatternAnalyzer extends Analyzer {
public final class PatternAnalyzer extends ReusableAnalyzerBase {
   
   /** <code>"\\W+"</code>; Divides text at non-letters (NOT Character.isLetter(c)) */
   public static final Pattern NON_WORD_PATTERN = Pattern.compile("\\W+");
@@ -187,25 +188,21 @@ public final class PatternAnalyzer extends Analyzer {
    *            the string to tokenize
    * @return a new token stream
    */
  public TokenStream tokenStream(String fieldName, String text) {
  public TokenStreamComponents createComponents(String fieldName, String text) {
     // Ideally the Analyzer superclass should have a method with the same signature, 
     // with a default impl that simply delegates to the StringReader flavour. 
     if (text == null) 
       throw new IllegalArgumentException("text must not be null");
     
    TokenStream stream;
     if (pattern == NON_WORD_PATTERN) { // fast path
      stream = new FastStringTokenizer(text, true, toLowerCase, stopWords);
      return new TokenStreamComponents(new FastStringTokenizer(text, true, toLowerCase, stopWords));
    } else if (pattern == WHITESPACE_PATTERN) { // fast path
      return new TokenStreamComponents(new FastStringTokenizer(text, false, toLowerCase, stopWords));
     }
    else if (pattern == WHITESPACE_PATTERN) { // fast path
      stream = new FastStringTokenizer(text, false, toLowerCase, stopWords);
    }
    else {
      stream = new PatternTokenizer(text, pattern, toLowerCase);
      if (stopWords != null) stream = new StopFilter(matchVersion, stream, stopWords);
    }
    
    return stream;

    Tokenizer tokenizer = new PatternTokenizer(text, pattern, toLowerCase);
    TokenStream result = (stopWords != null) ? new StopFilter(matchVersion, tokenizer, stopWords) : tokenizer;
    return new TokenStreamComponents(tokenizer, result);
   }
   
   /**
@@ -220,10 +217,10 @@ public final class PatternAnalyzer extends Analyzer {
    * @return a new token stream
    */
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
  public TokenStreamComponents createComponents(String fieldName, Reader reader) {
     try {
       String text = toString(reader);
      return tokenStream(fieldName, text);
      return createComponents(fieldName, text);
     } catch (IOException e) {
       throw new RuntimeException(e);
     }
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
index c856b524c0c..21415be5c3e 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/ClassicAnalyzer.java
@@ -123,9 +123,9 @@ public final class ClassicAnalyzer extends StopwordAnalyzerBase {
     tok = new StopFilter(matchVersion, tok, stopwords);
     return new TokenStreamComponents(src, tok) {
       @Override
      protected boolean reset(final Reader reader) throws IOException {
      protected void reset(final Reader reader) throws IOException {
         src.setMaxTokenLength(ClassicAnalyzer.this.maxTokenLength);
        return super.reset(reader);
        super.reset(reader);
       }
     };
   }
diff --git a/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java b/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
index 637e06994bd..12f185e4d98 100644
-- a/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
++ b/modules/analysis/common/src/java/org/apache/lucene/analysis/standard/StandardAnalyzer.java
@@ -124,9 +124,9 @@ public final class StandardAnalyzer extends StopwordAnalyzerBase {
     tok = new StopFilter(matchVersion, tok, stopwords);
     return new TokenStreamComponents(src, tok) {
       @Override
      protected boolean reset(final Reader reader) throws IOException {
      protected void reset(final Reader reader) throws IOException {
         src.setMaxTokenLength(StandardAnalyzer.this.maxTokenLength);
        return super.reset(reader);
        super.reset(reader);
       }
     };
   }
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
index 0f24353b0d4..3cec2995305 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/cn/TestChineseTokenizer.java
@@ -21,9 +21,7 @@ import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.util.Version;
@@ -66,10 +64,10 @@ public class TestChineseTokenizer extends BaseTokenStreamTestCase
      * Analyzer that just uses ChineseTokenizer, not ChineseFilter.
      * convenience to show the behavior of the tokenizer
      */
    private class JustChineseTokenizerAnalyzer extends Analyzer {
    private class JustChineseTokenizerAnalyzer extends ReusableAnalyzerBase {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new ChineseTokenizer(reader);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new ChineseTokenizer(reader));
       }   
     }
     
@@ -77,10 +75,11 @@ public class TestChineseTokenizer extends BaseTokenStreamTestCase
      * Analyzer that just uses ChineseFilter, not ChineseTokenizer.
      * convenience to show the behavior of the filter.
      */
    private class JustChineseFilterAnalyzer extends Analyzer {
    private class JustChineseFilterAnalyzer extends ReusableAnalyzerBase {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new ChineseFilter(new WhitespaceTokenizer(Version.LUCENE_CURRENT, reader));
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new WhitespaceTokenizer(Version.LUCENE_CURRENT, reader);
        return new TokenStreamComponents(tokenizer, new ChineseFilter(tokenizer));
       }
     }
     
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
index 7fac035fd91..2119fc4a909 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/commongrams/CommonGramsFilterTest.java
@@ -19,11 +19,8 @@ package org.apache.lucene.analysis.commongrams;
 import java.io.Reader;
 import java.io.StringReader;
 import java.util.Arrays;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.util.CharArraySet;
@@ -87,11 +84,12 @@ public class CommonGramsFilterTest extends BaseTokenStreamTestCase {
    * @return Map<String,String>
    */
   public void testCommonGramsQueryFilter() throws Exception {
    Analyzer a = new Analyzer() {    
    Analyzer a = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String field, Reader in) {
        return new CommonGramsQueryFilter(new CommonGramsFilter(TEST_VERSION_CURRENT,
            new MockTokenizer(in, MockTokenizer.WHITESPACE, false), commonWords));
      public TokenStreamComponents createComponents(String field, Reader in) {
        Tokenizer tokenizer = new MockTokenizer(in, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new CommonGramsQueryFilter(new CommonGramsFilter(TEST_VERSION_CURRENT,
            tokenizer, commonWords)));
       } 
     };
 
@@ -156,11 +154,12 @@ public class CommonGramsFilterTest extends BaseTokenStreamTestCase {
   }
   
   public void testCommonGramsFilter() throws Exception {
    Analyzer a = new Analyzer() {    
    Analyzer a = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String field, Reader in) {
        return new CommonGramsFilter(TEST_VERSION_CURRENT,
            new MockTokenizer(in, MockTokenizer.WHITESPACE, false), commonWords);
      public TokenStreamComponents createComponents(String field, Reader in) {
        Tokenizer tokenizer = new MockTokenizer(in, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new CommonGramsFilter(TEST_VERSION_CURRENT,
            tokenizer, commonWords));
       } 
     };
 
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
index 813c1195745..b3fed982ead 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/core/TestAnalyzers.java
@@ -21,10 +21,7 @@ import java.io.IOException;
 import java.io.Reader;
 import java.io.StringReader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.standard.StandardTokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
@@ -120,12 +117,12 @@ public class TestAnalyzers extends BaseTokenStreamTestCase {
     String[] y = StandardTokenizer.TOKEN_TYPES;
   }
 
  private static class LowerCaseWhitespaceAnalyzer extends Analyzer {
  private static class LowerCaseWhitespaceAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new LowerCaseFilter(TEST_VERSION_CURRENT,
          new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
      return new TokenStreamComponents(tokenizer, new LowerCaseFilter(TEST_VERSION_CURRENT, tokenizer));
     }
     
   }
@@ -237,4 +234,4 @@ final class PayloadSetter extends TokenFilter {
     data[0]++;
     return true;
   }
}
\ No newline at end of file
}
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/PatternAnalyzerTest.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/PatternAnalyzerTest.java
index a0a59f661ab..8d6c5cf5c6d 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/PatternAnalyzerTest.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/PatternAnalyzerTest.java
@@ -18,6 +18,7 @@ package org.apache.lucene.analysis.miscellaneous;
  */
 
 import java.io.IOException;
import java.io.StringReader;
 import java.util.Arrays;
 import java.util.regex.Pattern;
 
@@ -128,7 +129,7 @@ public class PatternAnalyzerTest extends BaseTokenStreamTestCase {
     assertTokenStreamContents(ts, expected);
 
     // analysis of a String, uses PatternAnalyzer.tokenStream(String, String)
    TokenStream ts2 = analyzer.tokenStream("dummy", document);
    TokenStream ts2 = analyzer.tokenStream("dummy", new StringReader(document));
     assertTokenStreamContents(ts2, expected);
   }
 }
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
index 77cf6efae17..c12a4142cc7 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/miscellaneous/TestWordDelimiterFilter.java
@@ -17,12 +17,7 @@
 
 package org.apache.lucene.analysis.miscellaneous;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Token;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.StopFilter;
 import org.apache.lucene.analysis.standard.StandardAnalyzer;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -245,13 +240,13 @@ public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {
         new int[] { 1, 1, 1 });
     
     /* analyzer that will consume tokens with large position increments */
    Analyzer a2 = new Analyzer() {
    Analyzer a2 = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String field, Reader reader) {
        return new WordDelimiterFilter(
            new LargePosIncTokenFilter(
            new MockTokenizer(reader, MockTokenizer.WHITESPACE, false)),
            flags, protWords);
      public TokenStreamComponents createComponents(String field, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(
            new LargePosIncTokenFilter(tokenizer),
            flags, protWords));
       }
     };
     
@@ -278,13 +273,14 @@ public class TestWordDelimiterFilter extends BaseTokenStreamTestCase {
         new int[] { 6, 14, 19 },
         new int[] { 1, 11, 1 });
 
    Analyzer a3 = new Analyzer() {
    Analyzer a3 = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String field, Reader reader) {
      public TokenStreamComponents createComponents(String field, Reader reader) {
        Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
         StopFilter filter = new StopFilter(TEST_VERSION_CURRENT,
            new MockTokenizer(reader, MockTokenizer.WHITESPACE, false), StandardAnalyzer.STOP_WORDS_SET);
            tokenizer, StandardAnalyzer.STOP_WORDS_SET);
         filter.setEnablePositionIncrements(true);
        return new WordDelimiterFilter(filter, flags, protWords);
        return new TokenStreamComponents(tokenizer, new WordDelimiterFilter(filter, flags, protWords));
       }
     };
 
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzerTest.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzerTest.java
index b3001335a49..a568537dc5e 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzerTest.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/query/QueryAutoStopWordAnalyzerTest.java
@@ -144,32 +144,6 @@ public class QueryAutoStopWordAnalyzerTest extends BaseTokenStreamTestCase {
     assertTokenStreamContents(protectedTokenStream, new String[]{"boring"});
   }
   
  /*
   * analyzer that does not support reuse
   * it is LetterTokenizer on odd invocations, WhitespaceTokenizer on even.
   */
  private class NonreusableAnalyzer extends Analyzer {
    int invocationCount = 0;
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      if (++invocationCount % 2 == 0)
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      else
        return new MockTokenizer(reader, MockTokenizer.SIMPLE, false);
    }
  }
  
  public void testWrappingNonReusableAnalyzer() throws Exception {
    QueryAutoStopWordAnalyzer a = new QueryAutoStopWordAnalyzer(TEST_VERSION_CURRENT, new NonreusableAnalyzer());
    a.addStopWords(reader, 10);

    TokenStream tokenStream = a.reusableTokenStream("repetitiveField", new StringReader("boring"));
    assertTokenStreamContents(tokenStream, new String[0]);

    tokenStream = a.reusableTokenStream("repetitiveField", new StringReader("vaguelyboring"));
    assertTokenStreamContents(tokenStream, new String[0]);
  }
  
   public void testTokenStream() throws Exception {
     QueryAutoStopWordAnalyzer a = new QueryAutoStopWordAnalyzer(TEST_VERSION_CURRENT, new MockAnalyzer(random, MockTokenizer.WHITESPACE, false));
     a.addStopWords(reader, 10);
diff --git a/modules/analysis/common/src/test/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapperTest.java b/modules/analysis/common/src/test/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapperTest.java
index ee5e5ed6379..e4b4f35fe03 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapperTest.java
++ b/modules/analysis/common/src/test/org/apache/lucene/analysis/shingle/ShingleAnalyzerWrapperTest.java
@@ -148,40 +148,6 @@ public class ShingleAnalyzerWrapperTest extends BaseTokenStreamTestCase {
         new int[] { 6, 9, 9, 12, 12, 18, 18 },
         new int[] { 1, 0, 1, 0, 1, 0, 1 });
   }
  
  /*
   * analyzer that does not support reuse
   * it is LetterTokenizer on odd invocations, WhitespaceTokenizer on even.
   */
  private class NonreusableAnalyzer extends Analyzer {
    int invocationCount = 0;
    @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      if (++invocationCount % 2 == 0)
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      else
        return new MockTokenizer(reader, MockTokenizer.SIMPLE, false);
    }
  }
  
  public void testWrappedAnalyzerDoesNotReuse() throws Exception {
    Analyzer a = new ShingleAnalyzerWrapper(new NonreusableAnalyzer());
    assertAnalyzesToReuse(a, "please divide into shingles.",
        new String[] { "please", "please divide", "divide", "divide into", "into", "into shingles", "shingles" },
        new int[] { 0, 0, 7, 7, 14, 14, 19 },
        new int[] { 6, 13, 13, 18, 18, 27, 27 },
        new int[] { 1, 0, 1, 0, 1, 0, 1 });
    assertAnalyzesToReuse(a, "please divide into shingles.",
        new String[] { "please", "please divide", "divide", "divide into", "into", "into shingles.", "shingles." },
        new int[] { 0, 0, 7, 7, 14, 14, 19 },
        new int[] { 6, 13, 13, 18, 18, 28, 28 },
        new int[] { 1, 0, 1, 0, 1, 0, 1 });
    assertAnalyzesToReuse(a, "please divide into shingles.",
        new String[] { "please", "please divide", "divide", "divide into", "into", "into shingles", "shingles" },
        new int[] { 0, 0, 7, 7, 14, 14, 19 },
        new int[] { 6, 13, 13, 18, 18, 27, 27 },
        new int[] { 1, 0, 1, 0, 1, 0, 1 });
  }
 
   public void testNonDefaultMinShingleSize() throws Exception {
     ShingleAnalyzerWrapper analyzer 
diff --git a/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java b/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
index 348cb307913..cb9c6e49214 100644
-- a/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
++ b/modules/analysis/common/src/test/org/apache/lucene/collation/TestCollationKeyFilter.java
@@ -18,9 +18,7 @@ package org.apache.lucene.collation;
  */
 
 
import org.apache.lucene.analysis.CollationTestBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.util.BytesRef;
 
@@ -54,7 +52,7 @@ public class TestCollationKeyFilter extends CollationTestBase {
     (collator.getCollationKey(secondRangeEndOriginal).toByteArray()));
 
   
  public final class TestAnalyzer extends Analyzer {
  public final class TestAnalyzer extends ReusableAnalyzerBase {
     private Collator _collator;
 
     TestAnalyzer(Collator collator) {
@@ -62,10 +60,9 @@ public class TestCollationKeyFilter extends CollationTestBase {
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new KeywordTokenizer(reader);
      result = new CollationKeyFilter(result, _collator);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new KeywordTokenizer(reader);
      return new TokenStreamComponents(result, new CollationKeyFilter(result, _collator));
     }
   }
 
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
index bc7a74d828c..9a632d6c280 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUFoldingFilter.java
@@ -20,20 +20,18 @@ package org.apache.lucene.analysis.icu;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 
 /**
  * Tests ICUFoldingFilter
  */
 public class TestICUFoldingFilter extends BaseTokenStreamTestCase {
  Analyzer a = new Analyzer() {
  Analyzer a = new ReusableAnalyzerBase() {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new ICUFoldingFilter(
          new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
      return new TokenStreamComponents(tokenizer, new ICUFoldingFilter(tokenizer));
     }
   };
   public void testDefaults() throws IOException {
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
index 1a503cdd95f..a7fbbaeb714 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/analysis/icu/TestICUNormalizer2Filter.java
@@ -20,9 +20,7 @@ package org.apache.lucene.analysis.icu;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.WhitespaceTokenizer;
 
 import com.ibm.icu.text.Normalizer2;
@@ -31,11 +29,11 @@ import com.ibm.icu.text.Normalizer2;
  * Tests the ICUNormalizer2Filter
  */
 public class TestICUNormalizer2Filter extends BaseTokenStreamTestCase {
  Analyzer a = new Analyzer() {
  Analyzer a = new ReusableAnalyzerBase() {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new ICUNormalizer2Filter(
          new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
      return new TokenStreamComponents(tokenizer, new ICUNormalizer2Filter(tokenizer));
     }
   };
 
@@ -61,13 +59,14 @@ public class TestICUNormalizer2Filter extends BaseTokenStreamTestCase {
   }
   
   public void testAlternate() throws IOException {
    Analyzer a = new Analyzer() {
    Analyzer a = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new ICUNormalizer2Filter(
            new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader),
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        Tokenizer tokenizer = new WhitespaceTokenizer(TEST_VERSION_CURRENT, reader);
        return new TokenStreamComponents(tokenizer, new ICUNormalizer2Filter(
            tokenizer,
             /* specify nfc with decompose to get nfd */
            Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.DECOMPOSE));
            Normalizer2.getInstance(null, "nfc", Normalizer2.Mode.DECOMPOSE)));
       }
     };
     
diff --git a/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java b/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
index 68b0ec92c0b..a513bba3b4f 100644
-- a/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
++ b/modules/analysis/icu/src/test/org/apache/lucene/collation/TestICUCollationKeyFilter.java
@@ -20,9 +20,7 @@ package org.apache.lucene.collation;
 
 import com.ibm.icu.text.Collator;
 
import org.apache.lucene.analysis.CollationTestBase;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.core.KeywordTokenizer;
 import org.apache.lucene.util.BytesRef;
 
@@ -46,7 +44,7 @@ public class TestICUCollationKeyFilter extends CollationTestBase {
     (collator.getCollationKey(secondRangeEndOriginal).toByteArray()));
 
   
  public final class TestAnalyzer extends Analyzer {
  public final class TestAnalyzer extends ReusableAnalyzerBase {
     private Collator _collator;
 
     TestAnalyzer(Collator collator) {
@@ -54,10 +52,9 @@ public class TestICUCollationKeyFilter extends CollationTestBase {
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new KeywordTokenizer(reader);
      result = new ICUCollationKeyFilter(result, _collator);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new KeywordTokenizer(reader);
      return new TokenStreamComponents(result, new ICUCollationKeyFilter(result, _collator));
     }
   }
 
diff --git a/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java b/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
index e42287935d5..d805717a34b 100644
-- a/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
++ b/modules/analysis/smartcn/src/java/org/apache/lucene/analysis/cn/smart/SmartChineseAnalyzer.java
@@ -25,6 +25,7 @@ import java.util.Collections;
 import java.util.Set;
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.en.PorterStemFilter;
 import org.apache.lucene.analysis.util.WordlistLoader;
 import org.apache.lucene.analysis.TokenStream;
@@ -54,7 +55,7 @@ import org.apache.lucene.util.Version;
  * </p>
  * @lucene.experimental
  */
public final class SmartChineseAnalyzer extends Analyzer {
public final class SmartChineseAnalyzer extends ReusableAnalyzerBase {
 
   private final Set<?> stopWords;
   
@@ -141,9 +142,9 @@ public final class SmartChineseAnalyzer extends Analyzer {
   }
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new SentenceTokenizer(reader);
    result = new WordTokenFilter(result);
  public TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer tokenizer = new SentenceTokenizer(reader);
    TokenStream result = new WordTokenFilter(tokenizer);
     // result = new LowerCaseFilter(result);
     // LowerCaseFilter is not needed, as SegTokenFilter lowercases Basic Latin text.
     // The porter stemming is too strict, this is not a bug, this is a feature:)
@@ -151,32 +152,6 @@ public final class SmartChineseAnalyzer extends Analyzer {
     if (!stopWords.isEmpty()) {
       result = new StopFilter(matchVersion, result, stopWords, false);
     }
    return result;
  }
  
  private static final class SavedStreams {
    Tokenizer tokenStream;
    TokenStream filteredTokenStream;
  }
  
  @Override
  public TokenStream reusableTokenStream(String fieldName, Reader reader)
      throws IOException {
    SavedStreams streams = (SavedStreams) getPreviousTokenStream();
    if (streams == null) {
      streams = new SavedStreams();
      setPreviousTokenStream(streams);
      streams.tokenStream = new SentenceTokenizer(reader);
      streams.filteredTokenStream = new WordTokenFilter(streams.tokenStream);
      streams.filteredTokenStream = new PorterStemFilter(streams.filteredTokenStream);
      if (!stopWords.isEmpty()) {
        streams.filteredTokenStream = new StopFilter(matchVersion, streams.filteredTokenStream, stopWords, false);
      }
    } else {
      streams.tokenStream.reset(reader);
      streams.filteredTokenStream.reset(); // reset WordTokenFilter's state
    }

    return streams.filteredTokenStream;
    return new TokenStreamComponents(tokenizer, result);
   }
 }
diff --git a/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java b/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
index 20b28a93eed..45103b6260d 100644
-- a/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
++ b/modules/facet/src/test/org/apache/lucene/facet/search/CategoryListIteratorTest.java
@@ -5,10 +5,7 @@ import java.io.Reader;
 import java.util.HashSet;
 import java.util.Set;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
 import org.apache.lucene.document.Document;
@@ -140,10 +137,10 @@ public class CategoryListIteratorTest extends LuceneTestCase {
     DataTokenStream dts2 = new DataTokenStream("2",new SortingIntEncoder(
         new UniqueValuesIntEncoder(new DGapIntEncoder(new VInt8IntEncoder()))));
     // this test requires that no payloads ever be randomly present!
    final Analyzer noPayloadsAnalyzer = new Analyzer() {
    final Analyzer noPayloadsAnalyzer = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MockTokenizer(reader, MockTokenizer.KEYWORD, false);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.KEYWORD, false));
       }
     };
     // NOTE: test is wired to LogMP... because test relies on certain docids having payloads
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
index 22b9df40774..1e900c0cc1e 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/analyzing/TestAnalyzingQueryParser.java
@@ -20,10 +20,7 @@ package org.apache.lucene.queryparser.analyzing;
 import java.io.IOException;
 import java.io.Reader;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.queryparser.classic.ParseException;
 import org.apache.lucene.util.LuceneTestCase;
@@ -137,14 +134,11 @@ final class TestFoldingFilter extends TokenFilter {
   }
 }
 
final class ASCIIAnalyzer extends org.apache.lucene.analysis.Analyzer {
  public ASCIIAnalyzer() {
  }
final class ASCIIAnalyzer extends ReusableAnalyzerBase {
 
   @Override
  public TokenStream tokenStream(String fieldName, Reader reader) {
    TokenStream result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
    result = new TestFoldingFilter(result);
    return result;
  public TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer result = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
    return new TokenStreamComponents(result, new TestFoldingFilter(result));
   }
 }
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
index 9ea9cf6e036..a845161f99d 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiAnalyzer.java
@@ -122,16 +122,12 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
    * Expands "multi" to "multi" and "multi2", both at the same position,
    * and expands "triplemulti" to "triplemulti", "multi3", and "multi2".  
    */
  private class MultiAnalyzer extends Analyzer {

    public MultiAnalyzer() {
    }
  private class MultiAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      result = new TestFilter(result);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new TestFilter(result));
     }
   }
 
@@ -196,16 +192,12 @@ public class TestMultiAnalyzer extends BaseTokenStreamTestCase {
    * Analyzes "the quick brown" as: quick(incr=2) brown(incr=1).
    * Does not work correctly for input other than "the quick brown ...".
    */
  private class PosIncrementAnalyzer extends Analyzer {

    public PosIncrementAnalyzer() {
    }
  private class PosIncrementAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      result = new TestPosIncrementFilter(result);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new TestPosIncrementFilter(result));
     }
   }
 
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
index 0bce46ce3bf..8ee4fcaf1b2 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiFieldQueryParser.java
@@ -22,9 +22,7 @@ import java.io.Reader;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.IndexWriter;
@@ -302,22 +300,23 @@ public class TestMultiFieldQueryParser extends LuceneTestCase {
   /**
    * Return empty tokens for field "f1".
    */
  private static class AnalyzerReturningNull extends Analyzer {
  private static class AnalyzerReturningNull extends ReusableAnalyzerBase {
     MockAnalyzer stdAnalyzer = new MockAnalyzer(random);
 
     public AnalyzerReturningNull() {
      super(new PerFieldReuseStrategy());
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       if ("f1".equals(fieldName)) {
        return new EmptyTokenStream();
        return new TokenStreamComponents(new EmptyTokenStream());
       } else {
        return stdAnalyzer.tokenStream(fieldName, reader);
        return stdAnalyzer.createComponents(fieldName, reader);
       }
     }
 
    private static class EmptyTokenStream extends TokenStream {
    private static class EmptyTokenStream extends Tokenizer {
       @Override
       public boolean incrementToken() throws IOException {
         return false;
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
index 3c8e963d384..8da47149fa2 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/classic/TestMultiPhraseQueryParsing.java
@@ -18,6 +18,7 @@ package org.apache.lucene.queryparser.classic;
  */
 
 import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
 import org.apache.lucene.analysis.TokenStream;
 import org.apache.lucene.analysis.Tokenizer;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
@@ -41,7 +42,7 @@ public class TestMultiPhraseQueryParsing extends LuceneTestCase {
       }
     }
 
  private static class CannedAnalyzer extends Analyzer {
  private static class CannedAnalyzer extends ReusableAnalyzerBase {
     private final TokenAndPos[] tokens;
 
     public CannedAnalyzer(TokenAndPos[] tokens) {
@@ -49,8 +50,8 @@ public class TestMultiPhraseQueryParsing extends LuceneTestCase {
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new CannedTokenizer(tokens);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new CannedTokenizer(tokens));
     }
   }
 
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
index e48c84bbfd1..86f27f95205 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/precedence/TestPrecedenceQueryParser.java
@@ -112,12 +112,13 @@ public class TestPrecedenceQueryParser extends LuceneTestCase {
     }
   }
 
  public static final class QPTestAnalyzer extends Analyzer {
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
      return new QPTestFilter(new MockTokenizer(reader, MockTokenizer.SIMPLE, true));
    public final TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(tokenizer, new QPTestFilter(tokenizer));
     }
   }
 
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
index 643183d78aa..65d4973d884 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiAnalyzerQPHelper.java
@@ -143,16 +143,12 @@ public class TestMultiAnalyzerQPHelper extends LuceneTestCase {
    * Expands "multi" to "multi" and "multi2", both at the same position, and
    * expands "triplemulti" to "triplemulti", "multi3", and "multi2".
    */
  private class MultiAnalyzer extends Analyzer {

    public MultiAnalyzer() {
    }
  private class MultiAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      result = new TestFilter(result);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new TestFilter(result));
     }
   }
 
@@ -214,16 +210,12 @@ public class TestMultiAnalyzerQPHelper extends LuceneTestCase {
    * Analyzes "the quick brown" as: quick(incr=2) brown(incr=1). Does not work
    * correctly for input other than "the quick brown ...".
    */
  private class PosIncrementAnalyzer extends Analyzer {

    public PosIncrementAnalyzer() {
    }
  private class PosIncrementAnalyzer extends ReusableAnalyzerBase {
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      TokenStream result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      result = new TestPosIncrementFilter(result);
      return result;
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer result = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(result, new TestPosIncrementFilter(result));
     }
   }
 
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
index cd5d7c5cd72..97ef084bde8 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestMultiFieldQPHelper.java
@@ -21,9 +21,7 @@ import java.io.Reader;
 import java.util.HashMap;
 import java.util.Map;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.document.Document;
 import org.apache.lucene.document.TextField;
 import org.apache.lucene.index.IndexWriter;
@@ -340,22 +338,23 @@ public class TestMultiFieldQPHelper extends LuceneTestCase {
   /**
    * Return empty tokens for field "f1".
    */
  private static final class AnalyzerReturningNull extends Analyzer {
  private static final class AnalyzerReturningNull extends ReusableAnalyzerBase {
     MockAnalyzer stdAnalyzer = new MockAnalyzer(random);
 
     public AnalyzerReturningNull() {
      super(new PerFieldReuseStrategy());
     }
 
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
       if ("f1".equals(fieldName)) {
        return new EmptyTokenStream();
        return new TokenStreamComponents(new EmptyTokenStream());
       } else {
        return stdAnalyzer.tokenStream(fieldName, reader);
        return stdAnalyzer.createComponents(fieldName, reader);
       }
     }
 
    private static class EmptyTokenStream extends TokenStream {
    private static class EmptyTokenStream extends Tokenizer {
       @Override
       public boolean incrementToken() {
         return false;
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
index 63b6489c192..6627609a5d5 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/flexible/standard/TestQPHelper.java
@@ -128,12 +128,13 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  public static final class QPTestAnalyzer extends Analyzer {
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
      return new QPTestFilter(new MockTokenizer(reader, MockTokenizer.SIMPLE, true));
    public final TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(tokenizer, new QPTestFilter(tokenizer));
     }
   }
 
@@ -344,10 +345,10 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  private class SimpleCJKAnalyzer extends Analyzer {
  private class SimpleCJKAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new SimpleCJKTokenizer(reader));
     }
   }
   
@@ -1241,10 +1242,10 @@ public class TestQPHelper extends LuceneTestCase {
     }
   }
 
  private class CannedAnalyzer extends Analyzer {
  private class CannedAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String ignored, Reader alsoIgnored) {
      return new CannedTokenStream();
    public TokenStreamComponents createComponents(String ignored, Reader alsoIgnored) {
      return new TokenStreamComponents(new CannedTokenStream());
     }
   }
 
diff --git a/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java b/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
index 801fbbb45c9..dae74708d2d 100644
-- a/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
++ b/modules/queryparser/src/test/org/apache/lucene/queryparser/util/QueryParserTestBase.java
@@ -25,13 +25,7 @@ import java.util.Date;
 import java.util.GregorianCalendar;
 import java.util.Locale;
 
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.MockAnalyzer;
import org.apache.lucene.analysis.MockTokenFilter;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.*;
 import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
 import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
 import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
@@ -104,12 +98,13 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
 
   
  public static final class QPTestAnalyzer extends Analyzer {
  public static final class QPTestAnalyzer extends ReusableAnalyzerBase {
 
     /** Filters MockTokenizer with StopFilter. */
     @Override
    public final TokenStream tokenStream(String fieldName, Reader reader) {
      return new QPTestFilter(new MockTokenizer(reader, MockTokenizer.SIMPLE, true));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.SIMPLE, true);
      return new TokenStreamComponents(tokenizer, new QPTestFilter(tokenizer));
     }
   }
 
@@ -245,10 +240,10 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
   }
 
  private class SimpleCJKAnalyzer extends Analyzer {
  private class SimpleCJKAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new SimpleCJKTokenizer(reader);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new SimpleCJKTokenizer(reader));
     }
   }
 
@@ -348,10 +343,10 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     assertQueryEquals("a OR -b", null, "a -b");
 
     // +,-,! should be directly adjacent to operand (i.e. not separated by whitespace) to be treated as an operator
    Analyzer a = new Analyzer() {
    Analyzer a = new ReusableAnalyzerBase() {
       @Override
      public TokenStream tokenStream(String fieldName, Reader reader) {
        return new MockTokenizer(reader, MockTokenizer.WHITESPACE, false);
      public TokenStreamComponents createComponents(String fieldName, Reader reader) {
        return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, false));
       }
     };
     assertQueryEquals("a - b", a, "a - b");
@@ -1162,18 +1157,19 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
   }
   
   /** whitespace+lowercase analyzer with synonyms */
  private class Analyzer1 extends Analyzer {
  private class Analyzer1 extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new MockSynonymFilter(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(tokenizer, new MockSynonymFilter(tokenizer));
     }
   }
   
   /** whitespace+lowercase analyzer without synonyms */
  private class Analyzer2 extends Analyzer {
  private class Analyzer2 extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      return new TokenStreamComponents(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
     }
   }
   
@@ -1235,10 +1231,11 @@ public abstract class QueryParserTestBase extends LuceneTestCase {
     }
     
   }
  private class MockCollationAnalyzer extends Analyzer {
  private class MockCollationAnalyzer extends ReusableAnalyzerBase {
     @Override
    public TokenStream tokenStream(String fieldName, Reader reader) {
      return new MockCollationFilter(new MockTokenizer(reader, MockTokenizer.WHITESPACE, true));
    public TokenStreamComponents createComponents(String fieldName, Reader reader) {
      Tokenizer tokenizer = new MockTokenizer(reader, MockTokenizer.WHITESPACE, true);
      return new TokenStreamComponents(tokenizer, new MockCollationFilter(tokenizer));
     }
   }
   
- 
2.19.1.windows.1

