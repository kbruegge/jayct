import pandas as pd
from sklearn.ensemble import ExtraTreesClassifier, ExtraTreesRegressor
from sklearn.model_selection import cross_val_score
import json
import click


def trees_to_json(clf, names, path):
    try:
        n_classes = clf.n_classes_
    except AttributeError:
        n_classes = clf.n_outputs_

    trees = [{
        'names': names,
        'thresholds': dt.tree_.threshold.tolist(),
        'attributes': dt.tree_.feature.tolist(),
        'children_left': dt.tree_.children_left.tolist(),
        'children_right': dt.tree_.children_right.tolist(),
        'node_distributions': dt.tree_.value.reshape(-1, n_classes).tolist(),
    } for dt in clf.estimators_]

    with open(path, 'w') as f:
        json.dump(trees, f)


@click.command()
@click.argument('gammas_path', type=click.Path(exists=True))
@click.argument('protons_path', type=click.Path(exists=True))
@click.argument('clf_path', type=click.Path(exists=False))
@click.argument('rgr_path', type=click.Path(exists=False))
def main(gammas_path, protons_path, clf_path, rgr_path):
    print('Building regressor')
    features = ['width', 'length', 'size', 'skewness', 'kurtosis', 'r', 'number_of_pixel']
    df = pd.read_csv(gammas_path).dropna()
    print(df[features].loc[0])
    df['label'] = 1


    X = df[features].values
    y = df.mc_energy.values
    print(X.shape)

    rgs = ExtraTreesRegressor(n_estimators=100, max_depth=10, n_jobs=4)
    rgs.fit(X, y)

    print('saving regressor')
    trees_to_json(rgs, features, rgr_path)

    print(' Building Classifier')
    df_protons = pd.read_csv(protons_path).dropna()
    df_protons['label'] = 0

    df = pd.concat([df, df_protons])
    X = df[features].values
    y = df['label'].values
    print(X.shape)

    clf = ExtraTreesClassifier(n_estimators=100, max_depth=10, n_jobs=4)
    clf.fit(X, y)

    print('saving classifier')
    trees_to_json(clf, features, clf_path)
    
    print(clf.predict_proba(X))

    print('Cross Val score AuC:')
    r = cross_val_score(clf, X, y, scoring='roc_auc', cv=5)
    print(r)


if __name__ == "__main__":
    main()