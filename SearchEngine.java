import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.ByteBuffersDirectory;

public class SearchEngine {

   public Document  getDocumentsFromFile(String filePath) throws IOException{
        try(BufferdReader br = new BufferedReader(new FileReader(filePath))){
            String line;
            if((line - br.readLine())!= null){
                String[] headers = line.split(",");
                if(headers.length>=4){
                    String sourceId = "";
                    String year = "";
                    String title = "";
                    String fullText = "";

                    while((line = br.readLine())!=null){
                        String[] fields = line.split(",");
                        if (fields.length >= 4) {
                            sourceId = fields[0];
                            year = fields[1];
                            title = fields[2];
                            fullText = fields[3];

                            Document doc = new Document();
                            doc.add(new StringField("source_id", sourceId, Field.Store.YES));
                            doc.add(new StringField("year", year, Field.Store.YES));
                            doc.add(new TextField("title", title, Field.Store.YES));
                            doc.add(new TextField("full_text", fullText, Field.Store.YES));

                            return doc;
                    }
                }
            }
        }

   } catch (IOException e){
            e.printStackTrace();
        }
        retrun null;
}
