
CREATE TABLE DhFullData(
	 DhSectionPos TEXT NOT NULL PRIMARY KEY
	 
    ,Data BLOB NULL
    
    ,CreatedDateTime DATETIME NOT NULL default CURRENT_TIMESTAMP -- in UTC
);

-- Note: each statement must be separated by the following batch comment line otherwise Java won't run anything after the first query
--batch--

CREATE TABLE DhRenderData(
	 DhSectionPos TEXT NOT NULL PRIMARY KEY
	 
    ,Data BLOB NULL
    
    ,CreatedDateTime DATETIME NOT NULL default CURRENT_TIMESTAMP -- in UTC
);
