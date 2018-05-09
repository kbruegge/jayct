import pandas as pd
import click
import matplotlib.pyplot as plt


@click.command()
@click.argument('input_csv', type=click.Path(exists=True))
@click.option('-o', '--output', type=click.Path(exists=False))
def main(input_csv, output):
    window_size_seconds = 5
    df = pd.read_csv(input_csv, skipfooter=2, engine='python', parse_dates=['timestamp'], header=None, names=['timestamp'])
    runtime_per_second = df.set_index('timestamp').resample(f'{window_size_seconds}s').size() / window_size_seconds

    plt.plot(runtime_per_second, '.')
    plt.ylim([0, runtime_per_second.max() + 4])
    if output:
        plt.savefig(output)
    else:
        plt.show()


if __name__ == '__main__':
    main()
