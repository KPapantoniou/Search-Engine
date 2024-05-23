package lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Searcher {

    private IndexSearcher indexSearcher;
    private QueryParser queryParser;
    private Analyzer analyzer;
    private Map<String, Float> boosts;

    public Searcher(String indexDir) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        IndexReader reader = DirectoryReader.open(dir);
        indexSearcher = new IndexSearcher(reader);
        analyzer = new StandardAnalyzer();

        boosts = new HashMap<>();
        boosts.put("title", 1.0f);
        boosts.put("abstract", 0.8f);
        boosts.put("full_text", 0.5f);

        queryParser = new MultiFieldQueryParser(new String[]{"title", "abstract", "full_text"}, analyzer, boosts);
    }

    public TopDocs search(String queryStr) throws Exception {
        Query query = queryParser.parse(QueryParser.escape(queryStr)); 
        return indexSearcher.search(query, Integer.MAX_VALUE); 
    }

    public TopDocs fieldSearch(String field, String queryStr) throws Exception {
        Query query = new QueryParser(field, analyzer).parse(QueryParser.escape(queryStr));
        return indexSearcher.search(query, Integer.MAX_VALUE); 
    }


    public TopDocs customSearch(String queryStr) throws Exception {
        return search(queryStr);
    }

    public Document getDocument(ScoreDoc scoreDoc) throws IOException {
        return indexSearcher.doc(scoreDoc.doc);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 2 || args.length > 3) {
            System.out.println("Put the correct arguments lil bro");
            return;
        }

        String indexDir = args[0];
        String queryStr = args[1];

        Searcher searcher = new Searcher(indexDir);

        if (args.length == 2) {

            TopDocs hits = searcher.search(queryStr);

            System.out.println("Found " + hits.totalHits + " hits.");
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document doc = searcher.getDocument(scoreDoc);
                System.out.println("Source ID: " + doc.get("source_id"));
                System.out.println("Year: " + doc.get("year"));
                System.out.println("Title: " + doc.get("title"));
                System.out.println("Abstract: " + doc.get("abstract"));
                String fullText = doc.get("full_text");
                int index = fullText.indexOf(queryStr);
                if (index != -1) {
                    int start = Math.max(0, index - 30);
                    int end = Math.min(fullText.length(), index + queryStr.length() + 30); 
                    String relevantPart = "..." + fullText.substring(start, end) + "...";
                    System.out.println("Relevant Part of Full Text: " + relevantPart);
                }
                System.out.println();
            }
        } else if (args.length == 3) {
           
            String field = args[2];
            TopDocs hits = searcher.fieldSearch(field, queryStr);

            if ("full_text".equals(field)) {
                System.out.println("Found " + hits.totalHits + " hits.");
                for (ScoreDoc scoreDoc : hits.scoreDocs) {
                    Document doc = searcher.getDocument(scoreDoc);
                    System.out.println("Source ID: " + doc.get("source_id"));
                    System.out.println("Year: " + doc.get("year"));
                    System.out.println("Title: " + doc.get("title"));
                    System.out.println("Abstract: " + doc.get("abstract"));
                    String fullText = doc.get("full_text");
                    int index = fullText.indexOf(queryStr);
                    if (index != -1) {
                        int start = Math.max(0, index - 30); 
                        int end = Math.min(fullText.length(), index + queryStr.length() + 30); 
                        String relevantPart = "..." + fullText.substring(start, end) + "...";
                        System.out.println("Relevant Part of Full Text: " + relevantPart);
                    }
                    System.out.println();
                }
            } else {
                System.out.println("Found " + hits.totalHits + " hits.");
                for (ScoreDoc scoreDoc : hits.scoreDocs) {
                    Document doc = searcher.getDocument(scoreDoc);
                    System.out.println("Source ID: " + doc.get("source_id"));
                    System.out.println("Year: " + doc.get("year"));
                    System.out.println("Title: " + doc.get("title"));
                }
            }
        }
    }



}

