# serpens
*SERPENS: SEaRch PEst and Nuisance Species (A CLARIAH Research Pilot Project)* 

_work in progress_

Historically, some animals have been perceived as threats by humans. These species were believed to carry diseases or harm crops and farm animals. SERPENS aims to study the historical impact of pest and nuisance species on human practices and changes in the public perception of these animals. The KB newspaper collection will be primary source of information to study this. The first impediment lies in accessing the KB collection: keyword search cannot smoothe spelling variations, vernacular vs. Latin names or the fact that many vernacular animal names can refer to other things (e.g. surnames). To remedy this, the WP2-3 diachronic lexicons will be used for query expansion in combination with topic modelling to filter out irrelevant results. Furthermore, instead of returning documents as search results, which are difficult to compare and analyze, we will create a database containing core information for studying human-fauna relationships by employing the WP3-Semantic-Parsing-Dutch tools.

Data
-----
Newspaper articles from the Dutch National Library's [Delpher](http://delpher.nl) portal are used in this project. The directory 'Annotations' contains spreadsheet files with information about individual newspaper articles such as their publication date, newspaper, article type and document identifier and whether the article reports on a nuisance species or the species name is used in a different context. We discern 8 categories: 

**Natural history** General articles about the animal, e.g. it subsists on birds or x number were stuffed and became part of a museum collection  
**Nuisance, material damages** The article mentions the animal as causing ma- terial damages, e.g. beetles damaging crops or lynxes killing chickens  
**Nuisance, immaterial damages** The article mentions the animal as a nui- sance without material damages e.g. polecats found to walk over someone’s face whilst they were in bed, or (possibly irrational) fear for a certain animal  
**Pest control** Organised hunt to bring down the number of pest species, e.g. ad for hunting dogs  
**Hunt for economic reasons** Hunting to use the fur, meat or other parts of the animal e.g. an article mentioning that the hunting season has started again and that polecat fur earns the hunter a good price  
**Prevention** Non-lethal actions against pest species, e.g. advice in the newspa- per on which plants keep away pest species  
**Accidents** The article mentions an unintentional encounter with the animal, e.g. roadkill  
**Figurative** Figurative language featuring the animal e.g. eyes like a lynx Other Articles not pertaining to the animal, e.g. a ship named ‘Lynx’ or a person whose last name is ‘Bunzing’  
