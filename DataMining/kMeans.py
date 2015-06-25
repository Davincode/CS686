__author__ = 'luchao'

import sys
import random
from sklearn import datasets
from sklearn.cross_validation import train_test_split
from sklearn.cluster import KMeans
import matplotlib.pyplot as plt
from sklearn import metrics

import math
import numpy as np


from sklearn import preprocessing


def calc_dist(X1, X2):
    """
    Calculate the Euclidean distance between the two given vectors
    :param X1: d-dimensional vector
    :param X2: d-dimensional vector
    :return: Euclidean distance between X1 and X2
    """
    # ## TODO: Your code here (Q1)
    result = 0
    if len(X1) != len(X2):
        return sys.maxint
    for i in range(len(X1)):
        result += math.pow(X1[i] - X2[i], 2)
    return math.sqrt(result)


def centroid(C):
    """
    Compute the centroid of the given list of vectors
    :param C: List of d-dimensional vectors
    :return: mean(C)
    """
    # ## TODO: Your code here (Q2)
    summary = {}
    for vector in C:
        for index in range(len(vector)):
            if index in summary:
                summary[index] += vector[index]
            else:
                summary[index] = vector[index]
    result = []
    for elem in sorted(summary):
        result.append(float(summary[elem]) / float(len(C)))
    return result


class kMeans():
    """
    This defines the kMeans clustering algorithm.
    """

    def __init__(self, k, max_iter=300):
        """

        :param k: the number of clusters
        :param max_iter: the maximum number of iterative updates to make to the cluster assignments;
          You can also stop before max_iter if you are satisfied with your clustering
        :return:
        """
        self.k = k
        self.iterations = max_iter
        #feel free to change the below assignment, but not the variable name,
        # this is where you should store your clusters
        self.clusters = None
        # this is where you should store your scaling object (see project description)
        self.scaler = None
        #you may add other class variables here
        self.ws = []

    def normalize(self, data):
        """
        Initializes self.scaler and returns a normalized version of the data
        :param data:
        :return:
        """
        # ## TODO: Your code here (Q3)
        self.scaler = preprocessing.MinMaxScaler()
        return self.scaler.fit_transform(data)

    def fit(self, data):
        """
        Find self.k clusters
        :param data: Unlabeled training data (list of x1...xd vectors)
        :return: None is fine, but feel free to return something meaningful to calling program
        """
        # ## TODO: Your code here (Q4)
        if self.clusters is None:
            self.clusters = {}
        for i in range(self.k):
            self.clusters[i] = []

        record = {}
        index = 0
        data = self.normalize(data)
        for vector in data:
            choice = random.randint(0, self.k - 1)
            self.clusters[choice].append(vector)
            record[index] = choice
            index += 1

        empty_index = 0
        for index in self.clusters:
            if len(self.clusters[index]) == 0:
                print "empty initialization"
                for i in range(1, self.k):
                    if len(self.clusters[(empty_index + i) % self.k]) > 1:
                        temp = self.clusters[(empty_index + i) % self.k].pop()
                        self.clusters[index].append(temp)
                        break
            empty_index += 1

        u = {}
        times = 0
        change = True
        while times < self.iterations and change:
            for i in self.clusters:
                u[i] = centroid(self.clusters[i])

            change = False
            index = 0
            for instance in data:
                minvalue = None
                assign = 0
                for i in u:
                    distance = calc_dist(instance, u[i])
                    if minvalue is None or distance < minvalue:
                        minvalue = distance
                        assign = i
                if assign != record[index]:
                    self.clusters[assign].append(instance)
                    self.clusters[record[index]] = [x for x in self.clusters[record[index]] if not np.array_equal(x, instance)]
                    record[index] = assign
                    change = True
                index += 1
            w = W(self.clusters)
            self.ws.append(w)
            print w
            times += 1

    def predict(self, data):
        """ Assumes that fit has already been called to create your clusters
        :param data: New data points
        :return: A list containing the index in range(k) to which each X in data should be assigned
        """
        # ## TODO: Your code here (Q5)

        # don't forget to apply self.scaler!
        u = {}
        for i in self.clusters:
            u[i] = centroid(self.clusters[i])

        result = []
        data = self.scaler.transform(data)
        for instance in data:
            minvalue = None
            assign = 0
            for i in u:
                distance = calc_dist(instance, u[i])
                if minvalue is None or distance < minvalue:
                    minvalue = distance
                    assign = i
            #self.clusters[assign].append(instance)
            result.append(assign)
        return np.array(result)


## DO NOT change this method
def eval_clustering(labels_true, labels_guess):
    """
    Given the ground truth and our guessed clustering assignment, use the Adjusted Rand index to measure
    assignment similarity
    :return: Rand Index
    """
    return metrics.adjusted_rand_score(labels_true, labels_guess)


def compare_sklearn(x_train, x_test, y_train, y_test, k):
    """
    Apply the KMeans algorithm of sklearn to the input data set, and return its "accuracy"
    of assigning labels to clusters. Use k as the number of clusters learned by sklearn's KMeans
    :param x_train:
    :param x_test:
    :param y_train:
    :param y_test:
    :return: Accuracy of the clustering assignments, using the training set accuracy if test set is empty
    """
    # ## TODO: Your code here (Q6)

    # this code will call eval_clustering; see main for how to use
    clu = KMeans(n_clusters=k)
    clu.fit(x_train)

    if len(x_test) > 0:
        guess_clusters = clu.predict(x_test)
        truth = y_test

        print guess_clusters
        print truth

    else:
        guess_clusters = clu.predict(x_train)
        truth = y_train

    return eval_clustering(truth, guess_clusters)


def default(str):
    return str + ' [Default: %default]'


def W(clustering):
    w = 0
    for index in clustering:
        distance_sum = 0
        for p in clustering[index]:
            for q in clustering[index]:
                if len(p) == len(q):
                    for i in range(len(p)):
                        distance_sum += math.pow(p[i] - q[i], 2)
        if len(clustering[index]) != 0:
            w += float(distance_sum) / float(len(clustering[index]))
    return w


def readCommand(argv):
    "Processes the command used to run from the command line."
    from optparse import OptionParser

    parser = OptionParser(USAGE_STRING)

    parser.add_option('-d', '--data', help=default('Dataset to use'), choices=['digits', 'iris'], default='iris')
    parser.add_option('-t', '--training', help=default('The size of the training set'), default=150, type="int")
    parser.add_option('-k', '--num_clusters', help=default("Number of clusters"), default=3, type="int")
    parser.add_option('-s', '--sk_compare', help=default("Compare to sklearn"), default=False)
    #feel free to add an option to change the max_iter parameter

    options, args = parser.parse_args(argv)

    if options.data != 'iris' and options.data != 'digits':
        print "Unknown dataset", options.data
        print USAGE_STRING
        sys.exit(2)

    if options.training < 0:
        print "Training set size should be zero or a positive integer (you provided: %d)" % options.training
        print USAGE_STRING
        sys.exit(2)

    if options.num_clusters <= 0:
        print "Please provide a positive number for number of clusters (you provided: %f)" % options.smoothing
        print USAGE_STRING
        sys.exit(2)

    return options

USAGE_STRING = """
  USAGE:      python kMeans.py <options>
  EXAMPLES:   (1) python kMeans.py
                  - trains the kMeans algorithm on the sklearn iris dataset with k=3
                  using the iris training examples and print the cluster membership on the same data

              (2) python kMeans.py  -d digits -t 1250 -k 10
                  - would run kMeans on a randomly chosen set of 1250 training examples of digits, with k=10, and
                  print the cluster membership on the held out (test/unseen) examples from digits
                 """


def runClustering(options):
    if options.data == 'iris':
        data_dict = datasets.load_iris()
    else:
        data_dict = datasets.load_digits()

    rawTrain = data_dict.data
    if options.training > len(rawTrain):
        print "Training set size you provided is more than the number of examples available " \
              "(you provided: %d, num exs: %d" % (options.training, len(rawTrain))
        sys.exit(2)

    if options.training == len(rawTrain):
        x_train = rawTrain
        x_test = []
        y_train = data_dict.target
        y_test = []
    else:
        x_train, x_test, y_train, y_test = train_test_split(rawTrain, data_dict.target, train_size=options.training)

    clf = kMeans(k=options.num_clusters)
    print "Training..."
    clf.fit(x_train)

    print "Testing..."
    if len(x_test) > 0:
        guess_clusters = clf.predict(x_test)
        truth = y_test

        print guess_clusters
        print truth
    else:
        guess_clusters = clf.predict(x_train)
        truth = y_train

    correct = eval_clustering(truth, guess_clusters)
    print "Score for your partitioning with %d training examples: %.1f%% " % (len(truth), correct * 100)

    if options.sk_compare:
        sk_correct = compare_sklearn(x_train, x_test, y_train, y_test, options.num_clusters)
        print "Score for sklearn's partitioning with %d training examples: %.1f%% " % (len(truth), sk_correct * 100)
        if sk_correct > correct:
            print "You lose!"
        else:
            print "You win!"

    traceX = []
    traceY = clf.ws
    for i in range(len(traceY)):
        traceX.append(i + 1)
    plt.plot(traceX, traceY)
    plt.show()

if __name__ == '__main__':
    # Read input
    options = readCommand(sys.argv[1:])
    # Run clustering
    runClustering(options)