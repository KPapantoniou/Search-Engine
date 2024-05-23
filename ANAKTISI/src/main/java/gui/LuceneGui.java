package gui;

import javax.swing.*;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import com.opencsv.exceptions.CsvValidationException;

import lucene.Indexer;
import lucene.Searcher;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class LuceneGui extends JFrame {
    private Indexer indexer;
    private Searcher searcher;
    private JTextArea logArea;
    private JTextField queryField;
    private JComboBox<String> searchTypeBox;
    private JButton searchButton, nextButton, sortYearButton, sortTitleButton;
    private JPanel navigationPanel;
    private static final String DATASET_PATH = "D:\\panepistimio\\anakthsh\\papers.csv";
    private static final String INDEX_DIR = "D:\\panepistimio\\anakthsh\\index";
    private static final String LOG_FILE = "log.txt";
    private TopDocs currentHits;
    private int currentPage;
    private static final int RESULTS_PER_PAGE = 10;
    private boolean isSortedByYear = false;
    private boolean isSortedAlphabetically = false;

    public LuceneGui() {
        setTitle("Lucene Indexer and Searcher");
        setSize(800, 600);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        logArea = new JTextArea();
        logArea.setEditable(false);
        add(new JScrollPane(logArea), BorderLayout.CENTER);

        JPanel queryPanel = new JPanel(new BorderLayout());
        queryField = new JTextField();
        queryPanel.add(queryField, BorderLayout.CENTER);

        searchTypeBox = new JComboBox<>(new String[]{"Keyword Search", "Field Search"});
        queryPanel.add(searchTypeBox, BorderLayout.EAST);

        searchButton = new JButton("Search");
        searchButton.addActionListener(new SearchAction());
        queryPanel.add(searchButton, BorderLayout.SOUTH);

        add(queryPanel, BorderLayout.NORTH);

        navigationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        nextButton = new JButton("Next");
        nextButton.addActionListener(new NextAction());
        nextButton.setEnabled(false);
        navigationPanel.add(nextButton);

        sortYearButton = new JButton("Sort by Year");
        sortYearButton.addActionListener(new SortByYearAction());
        sortYearButton.setEnabled(false);
        navigationPanel.add(sortYearButton);

        sortTitleButton = new JButton("Sort Alphabetically");
        sortTitleButton.addActionListener(new SortAlphabeticallyAction());
        sortTitleButton.setEnabled(false);
        navigationPanel.add(sortTitleButton);

        add(navigationPanel, BorderLayout.SOUTH);

        initializeIndexer();
        initializeSearcher();
    }

    private void initializeIndexer() {
        try {
            log("Initializing indexer...");
            indexer = new Indexer(INDEX_DIR, LOG_FILE);
            indexer.indexDataset(DATASET_PATH);
            indexer.close();
            log("Indexing completed.");
        } catch (IOException | CsvValidationException e) {
            log("Error initializing indexer: " + e.getMessage());
        }
    }

    private void initializeSearcher() {
        try {
            log("Initializing searcher...");
            searcher = new Searcher(INDEX_DIR);
            log("Searcher initialized.");
        } catch (IOException e) {
            log("Error initializing searcher: " + e.getMessage());
        }
    }

    private void log(String message) {
        logArea.append(message + "\n");
    }

    private class SearchAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            String queryStr = queryField.getText();
            String searchType = (String) searchTypeBox.getSelectedItem();

            if (queryStr.isEmpty()) {
                log("Query cannot be empty.");
                return;
            }

            try {
                if (searchType.equals("Keyword Search")) {
                    performKeywordSearch(queryStr);
                } else {
                    String field = JOptionPane.showInputDialog(LuceneGui.this, "Enter the field to search:", "Field Search", JOptionPane.PLAIN_MESSAGE);
                    if (field != null && !field.isEmpty()) {
                        performFieldSearch(field, queryStr);
                    } else {
                        log("Field cannot be empty.");
                    }
                }
                isSortedByYear = false;
                isSortedAlphabetically = false;
                sortYearButton.setEnabled(true);
                sortTitleButton.setEnabled(true);
            } catch (Exception ex) {
                log("Error performing search: " + ex.getMessage());
            }
        }

        private void performKeywordSearch(String queryStr) throws Exception {
            log("Performing keyword search for: " + queryStr);
            currentHits = searcher.search(queryStr);
            currentPage = 0;
            displaySearchResults(queryStr);
        }

        private void performFieldSearch(String field, String queryStr) throws Exception {
            log("Performing field search for: " + queryStr + " in field: " + field);
            currentHits = searcher.fieldSearch(field, queryStr);
            currentPage = 0;
            displaySearchResults(queryStr);
        }
    }

    private void displaySearchResults(String queryStr) throws IOException {
        logArea.setText("");
        int start = currentPage * RESULTS_PER_PAGE;
        int end = Math.min(start + RESULTS_PER_PAGE, currentHits.scoreDocs.length);
        log("Displaying results " + (start + 1) + " to " + end + " out of " + currentHits.totalHits.value + " hits.");

        for (int i = start; i < end; i++) {
            ScoreDoc scoreDoc = currentHits.scoreDocs[i];
            Document doc = searcher.getDocument(scoreDoc);
            logResult(doc, queryStr);
        }

        nextButton.setEnabled(end < currentHits.totalHits.value);
    }

    private void logResult(Document doc, String queryStr) throws IOException {
        log("Source ID: " + doc.get("source_id"));
        log("Year: " + doc.get("year"));
        log("Title: " + doc.get("title"));
        log("Abstract: " + doc.get("abstract"));
        String fullText = doc.get("full_text");
        int index = fullText.indexOf(queryStr);
        if (index != -1) {
            int start = Math.max(0, index - 30);
            int end = Math.min(fullText.length(), index + queryStr.length() + 30);
            String relevantPart = "..." + fullText.substring(start, end) + "...";
            log("Relevant Part of Full Text: " + relevantPart);
        }
        log("\n");

        highlightKeyword(queryStr);
    }

    private void highlightKeyword(String keyword) {
        Highlighter highlighter = logArea.getHighlighter();
        Highlighter.HighlightPainter painter = new DefaultHighlightPainter(Color.YELLOW);
        String text = logArea.getText();
        int index = text.indexOf(keyword);
        while (index >= 0) {
            try {
                highlighter.addHighlight(index, index + keyword.length(), painter);
                index = text.indexOf(keyword, index + keyword.length());
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }

    private class NextAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            currentPage++;
            try {
                displaySearchResults(queryField.getText());
            } catch (IOException ioException) {
                log("Error displaying next results: " + ioException.getMessage());
            }
        }
    }

    private class SortByYearAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentHits != null) {
                List<ScoreDoc> scoreDocList = Arrays.asList(currentHits.scoreDocs);
                scoreDocList.sort(new Comparator<ScoreDoc>() {
                    @Override
                    public int compare(ScoreDoc o1, ScoreDoc o2) {
                        try {
                            int year1 = Integer.parseInt(searcher.getDocument(o1).get("year"));
                            int year2 = Integer.parseInt(searcher.getDocument(o2).get("year"));
                            return Integer.compare(year1, year2);
                        } catch (IOException ex) {
                            return 0;
                        }
                    }
                });
                currentHits.scoreDocs = scoreDocList.toArray(new ScoreDoc[0]);
                currentPage = 0;
                try {
                    displaySearchResults(queryField.getText());
                } catch (IOException ioException) {
                    log("Error displaying sorted results: " + ioException.getMessage());
                }
                isSortedByYear = true;
                isSortedAlphabetically = false;
            }
        }
    }

    private class SortAlphabeticallyAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (currentHits != null) {
                List<ScoreDoc> scoreDocList = Arrays.asList(currentHits.scoreDocs);
                scoreDocList.sort(new Comparator<ScoreDoc>() {
                    @Override
                    public int compare(ScoreDoc o1, ScoreDoc o2) {
                        try {
                            String title1 = searcher.getDocument(o1).get("title");
                            String title2 = searcher.getDocument(o2).get("title");
                            return title1.compareToIgnoreCase(title2);
                        } catch (IOException ex) {
                            return 0;
                        }
                    }
                });
                currentHits.scoreDocs = scoreDocList.toArray(new ScoreDoc[0]);
                currentPage = 0;
                try {
                    displaySearchResults(queryField.getText());
                } catch (IOException ioException) {
                    log("Error displaying sorted results: " + ioException.getMessage());
                }
                isSortedByYear = false;
                isSortedAlphabetically = true;
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                LuceneGui gui = new LuceneGui();
                gui.setVisible(true);
            }
        });
    }
}
