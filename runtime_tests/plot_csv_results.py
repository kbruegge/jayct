import pandas as pd
import glob
from natsort import natsorted
import seaborn as sns
import click
import matplotlib.pyplot as plt

@click.command()
@click.argument('input_folder', type=click.Path(exists=True))
@click.option('-o', '--output_file', type=click.Path())
def main(input_folder, output_file):
    events_per_second = []
    files = natsorted(glob.glob('{}/*.csv'.format(input_folder)))

    resample_window_seconds = 30

    for i, f in enumerate(files):
        df = pd.read_csv(f, names=['timestamp', 'x', 'y', 'z', 'gammaness'], index_col='timestamp', parse_dates=True)
        e = df.gammaness.resample('{}s'.format(resample_window_seconds)).count().values / resample_window_seconds
        events_per_second.append(list(e))

    df  = pd.DataFrame(events_per_second).T
    df.columns = df.columns.values + 1
    sns.stripplot(data=df)
    if output_file:
        plt.savefig(output_file)
    else:
        plt.show()

if __name__=='__main__':
    main()
