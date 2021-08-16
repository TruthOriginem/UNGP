from numpy.core.numeric import NaN
import pandas as pd
import os
origin_csv_file = "UNGP_rules.csv"
compare_csv_file = "UNGP_rules_ENG.csv"

origin_df = pd.read_csv(origin_csv_file).set_index('id')
compare_df = pd.read_csv(compare_csv_file).set_index('id')

for id, item in origin_df.iterrows():
    if id and str(id) != 'nan' and not str(id).startswith('#'):
        origin_df.loc[id] = compare_df.loc[id]

origin_df.to_csv(compare_csv_file)