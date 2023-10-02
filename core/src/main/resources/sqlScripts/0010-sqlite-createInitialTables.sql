
-- TODO should track ran scripts
CREATE TABLE IF NOT EXISTS DhFullData(
	 DhSectionPos TEXT NOT NULL PRIMARY KEY
	 
    ,Data BLOB NULL
    
    --,CreatedDateTime DATETIME NOT NULL default CURRENT_TIMESTAMP -- in UTC
);
