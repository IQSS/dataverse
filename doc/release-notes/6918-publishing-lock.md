The setting :PIDAsynchRegFileCount is deprecated as of v5.0. 

It used to specify the number of datafiles in the dataset to warrant
adding a lock during publishing. As of v5.0 all datasets get
locked for the duration of the publishing process. The setting will be
ignored if present.
