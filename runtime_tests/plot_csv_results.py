import pandas as pd
df  = pd.read_csv('test.csv', names=['timestamp', 'x', 'y', 'z'], index_col='timestamp', parse_dates=True)
df = df.resample('5s').count()