package lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import org.apache.commons.csv.*;

public class Indexer {
    private IndexWriter writer;
    private PrintWriter logWriter;
    private static final Pattern LATIN_PATTERN = Pattern.compile("^[\\p{L}\\p{N}\\p{P}\\p{Z}\\p{M}\\s]+$");

    public Indexer(String indexDir, String logFilePath) throws IOException {
        Directory dir = FSDirectory.open(Paths.get(indexDir));
        Analyzer analyzer = new StandardAnalyzer();
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        writer = new IndexWriter(dir, config);
        logWriter = new PrintWriter(logFilePath, StandardCharsets.UTF_8);
    }

    public void close() throws IOException {
        writer.close();
        logWriter.close();
    }

    public void indexFile(String sourceId, String year, String title, String abstractText, String fullText) throws IOException {
        Document doc = new Document();
        doc.add(new StringField("source_id", sourceId, Field.Store.YES));
        doc.add(new StringField("year", year, Field.Store.YES));
        doc.add(new TextField("title", title, Field.Store.YES));
        doc.add(new TextField("abstract", abstractText, Field.Store.YES));
        doc.add(new TextField("full_text", fullText, Field.Store.YES));
        writer.addDocument(doc);

        
        logWriter.println("Abstract: " + abstractText);
        logWriter.println("Full Text: " + fullText);
        logWriter.flush(); 
    }



    public void indexDataset(String datasetPath) throws IOException, CsvValidationException {
        final int maxLines = 400;
        int currentLine = 0;
        boolean isFullText = false;
        StringBuilder entryBuilder = new StringBuilder();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(datasetPath), StandardCharsets.UTF_8))) {
            CSVParser parser = CSVFormat.DEFAULT.parse(br);
            
            for (CSVRecord record : parser) {
                currentLine++;

                
                if (currentLine == 1) {
                    continue;
                }

                
                logWriter.println("Processing line " + currentLine + ": " + record);
                logWriter.flush();

                
                String sourceId = record.get(0);
                String year = record.get(1);
                String title = record.get(2);
                String abstractText = record.get(3);
                String fullText = record.get(4);

                
                indexFile(sourceId, year, title, abstractText, fullText);

                if (currentLine >= maxLines) {
                    break;
                }
            }
        } catch (IOException e) {
            logWriter.println("Error processing CSV: " + e.getMessage());
            logWriter.flush();
            throw e;
        }
    }




    private void processEntry(String entry) throws IOException {
       
        CSVParser parser = CSVParser.parse(entry, CSVFormat.DEFAULT);
        CSVRecord record = parser.iterator().next(); 
        
        
        String sourceId = record.get(0);
        String year = record.get(1);
        String title = record.get(2);
        String abstractText = record.get(3);
        String fullText = record.get(4);

        
        indexFile(sourceId, year, title, abstractText, fullText);
    }




    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java com.example.lucene.Indexer <index dir> <dataset file> <log file>");
            return;
        }

        String indexDir = args[0];
        String datasetPath = args[1];
        String logFilePath = args[2];

        Indexer indexer = new Indexer(indexDir, logFilePath);
        indexer.indexDataset(datasetPath);
        indexer.close();
    }
}

