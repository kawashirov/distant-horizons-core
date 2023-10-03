
All Sql scripts should be run exactly once per database and old scripts shouldn't be changed. Any necessary schema changes should be done by creating new scripts that modify the existing database.

This system is roughly based on the DbUp library from .NET, for information about DbUp and it's general philosophy please refer to the following doc:
https://dbup.readthedocs.io/en/latest/philosophy-behind-dbup/


File naming scheme:
- The first 3 numbers are major scripts.
- The 4th number is for minor/related scripts or if a bug fix needs to be applied between scripts.
- database type the script is for (for now this will just be sqlite)
- description of the script


Note: Currently the repo can only handle 1 statement per script so each file can only contain 1 statement, any subsequent statements are ignored.
