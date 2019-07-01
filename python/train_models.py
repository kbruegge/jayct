import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from sklearn.ensemble import ExtraTreesClassifier, ExtraTreesRegressor
from sklearn.model_selection import cross_val_score
import json


def trees_to_json(clf, path):
    try:
        n_classes = clf.n_classes_
    except AttributeError:
        n_classes = clf.n_outputs_

    trees = [{
        'thresholds': dt.tree_.threshold.tolist(),
        'attributes': dt.tree_.feature.tolist(),
        'children_left': dt.tree_.children_left.tolist(),
        'children_right': dt.tree_.children_right.tolist(),
        'node_distributions': dt.tree_.value.reshape(-1, n_classes).tolist(),
    } for dt in clf.estimators_]

    with open(path, 'w') as f:
        json.dump(trees, f)



df = pd.read_csv('../gamma_dl2.csv').dropna()

X = df[['width', 'length', 'size', 'skewness', 'kurtosis', 'r', 'number_of_pixel']].values
y = df.mc_energy.values

rgs = ExtraTreesRegressor(n_estimators=100, max_depth=5, n_jobs=4)
rgs.fit(X, y)

trees_to_json(rgs, 'test_rgrs.json')