import json
import click
import logging
from sklearn.externals import joblib


@click.command()
@click.argument('input_path', type=click.Path(exists=True, dir_okay=False))
@click.argument('output_path', type=click.Path(exists=False, dir_okay=False))
def main(input_path, output_path):
    '''
    Convert pickled RandomForestClassifier to json format with no name.
    Write the result to OUTPUT_PATH.
    '''
    log = logging.getLogger()

    log.info('Loading pickled model at {}'.format(input_path))
    clf = joblib.load(input_path)

    log.info('Writing json model to {}'.format(output_path))
    trees_to_json(clf, path=output_path)


def trees_to_json(clf, path):
    n_classes = clf.n_classes_
    trees = [{
        'thresholds': dt.tree_.threshold.tolist(),
        'attributes': dt.tree_.feature.tolist(),
        'children_left': dt.tree_.children_left.tolist(),
        'children_right': dt.tree_.children_right.tolist(),
        'node_distributions': dt.tree_.value.reshape(-1, n_classes).tolist(),
    } for dt in clf.estimators_]

    with open(path, 'w') as f:
        json.dump(trees, f)


if __name__ == '__main__':
    main()
