Search Engine Application
This project is a search engine application built using Apache Lucene. It allows you to index and search academic papers stored in a CSV file.

Setup Instructions
Modify Configuration Paths:

Open LuceneGui.java.
Update the DATASET_PATH variable to the location of your paper.csv file.
Update the INDEX_DIR variable to the desired location for the index directory.
Run the Application:

Execute LuceneGui as a Java Application in Eclipse.
Usage
Search by Keyword:

Enter a word in the search bar.
Use the buttons at the bottom to sort the search results.
Search by Field:

You can search by specific fields such as source_id, year, title, abstract, and full_text.
Fields Explanation
source_id: Unique identifier for the source.
year: Year of publication.
title: Title of the paper.
abstract: Summary of the paper.
full_text: Complete text of the paper.
