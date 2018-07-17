Data 
----

This directory contains the metadata of the data used in the experiments. The *tsv files contain descriptions of articles that were returned by the api to a keyword that can denote an animal species. The species name is given as the name of the file, however, the query could also have matched a spelling variant of the species name. 

The columns in the files contain the following information:

* Column 1: article identifier 
* Column 2: publication year 
* Column 3: publication title 
* Column 4: article title 
* Column 5: document type (article, advertisement, family announcement)
* Column 6: Lexicon-based OCR score 
* Column 7: Identified language
* Column 8: Text snippet surrounding keyword match (REMOVED)
* Column 9: Full text (REMOVED)
* Column 10: SERPENS category annotation (01_Natural_History - 12_Bagger_OCR) 
* Column 11: Indication of a regional or local newspaper

The snippets and full article texts have been removed from these files due to copyright restrictions. From the article IDs, the article can be retrieved and these columns can be added back in.  

For more instructions on building up the dataset and obtaining access to the full text, see the README in /code/datamanagement 


