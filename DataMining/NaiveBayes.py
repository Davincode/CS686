import util
import classificationMethod
import math


class NaiveBayesClassifier(classificationMethod.ClassificationMethod):
    """
    See the project description for the specifications of the Naive Bayes classifier.

    Note that the variable 'datum' in this code refers to a counter of features
    (not to a raw samples.Datum).
    """
    def __init__(self, legalLabels):
        self.legalLabels = legalLabels
        self.type = "naivebayes"
        self.k = 0  # this is the smoothing parameter, ** use it in your train method **
        self.automaticTuning = False  # Look at this flag to decide whether to choose k automatically ** use this in your train method **

        self.labels = {}
        self.dict = {}
        self.temp_1 = {}
        self.temp_2 = {}

    def setSmoothing(self, k):
        """
        This is used by the main method to change the smoothing parameter before training.
        Do not modify this method.
        """
        self.k = k

    def train(self, trainingData, trainingLabels, validationData, validationLabels):
        """
        Outside shell to call your method. Do not modify this method.
        """

        self.features = trainingData[0].keys()  # this could be useful for your code later...

        if self.automaticTuning:
            kgrid = [0.001, 0.01, 0.05, 0.1, 0.5, 1, 5, 10, 20, 50]
            self.trainAndTune(trainingData, trainingLabels, validationData, validationLabels, kgrid)
        else:
            #two choices, either we have smoothing or we don't
            if self.k == 0:
                self.justTrain(trainingData, trainingLabels)
            else:
                self.trainAndTune(trainingData, trainingLabels, validationData, validationLabels, [self.k])

    def justTrain(self, trainingData, trainingLabels):
        """
           Trains the classifier by collecting counts over the training data, and
        stores the (unsmoothed) estimates so that they can be used to classify.

        trainingData and validationData are lists of feature Counters.  The corresponding
        label lists contain the correct label for each datum.

        To get the list of all possible features or labels, use self.features and
        self.legalLabels.
        """

        "*** YOUR CODE HERE ***"

        for label in self.legalLabels:
            self.labels[label] = 0

        for label in trainingLabels:
            if label in self.legalLabels:
                self.labels[label] += 1

        for label in self.legalLabels:
            self.dict[label] = {}
            for pixel_location in self.features:
                self.dict[label][pixel_location] = {}
                for i in range(2):
                    self.dict[label][pixel_location][i] = 0

        count = 0
        for data in trainingData:
            for pixel_location in data:
                value = data[pixel_location]
                current_label = trainingLabels[count]
                self.dict[current_label][pixel_location][value] += 1
            count += 1

        s = 0
        for label in self.labels:
            s += self.labels[label]

        for label in self.labels:
            if self.labels[label] and s:
                self.temp_1[label] = float(self.labels[label]) / float(s)
            else:
                self.temp_1[label] = 0

        for label in self.legalLabels:
            self.temp_2[label] = {}
            for pixel_location in self.features:
                self.temp_2[label][pixel_location] = {}
                for value in range(2):
                    x = self.dict[label][pixel_location][value] + self.k
                    y = self.labels[label] + 2 * self.k
                    if x and y:
                        self.temp_2[label][pixel_location][value] = float(x) / float(y)
                    else:
                        self.temp_2[label][pixel_location][value] = 0

    def trainAndTune(self, trainingData, trainingLabels, validationData, validationLabels, kgrid):
        """
        Trains the classifier by collecting counts over the training data, and
        stores the Laplace smoothed estimates so that they can be used to classify.
        Evaluate each value of k in kgrid to choose the smoothing parameter
        that gives the best accuracy on the held-out validationData.
        If kgrid just contains one value, use it for smoothing but then there's no need to evaluate
        on the validationData.

        trainingData and validationData are lists of feature Counters.  The corresponding
        label lists contain the correct label for each datum.

        To get the list of all possible features or labels, use self.features and
        self.legalLabels.
        """

        "*** YOUR CODE HERE ***"
        maxvalue = util.Counter()
        if len(kgrid) != 1:
            self.justTrain(validationData, validationLabels)
            for k in kgrid:
                self.temp_2.clear()
                for label in self.legalLabels:
                    self.temp_2[label] = {}
                    for pixel_location in self.features:
                        self.temp_2[label][pixel_location] = {}
                        for i in range(2):
                            self.temp_2[label][pixel_location][i] = 0

                for label in self.legalLabels:
                    self.temp_2[label] = {}
                    for pixel_location in self.features:
                        self.temp_2[label][pixel_location] = {}
                        for value in range(2):
                            x = self.dict[label][pixel_location][value] + k
                            y = self.labels[label] + 2 * k
                            if x and y:
                                self.temp_2[label][pixel_location][value] = float(x) / float(y)
                            else:
                                self.temp_2[label][pixel_location][value] = 0

                guesses = self.classify(validationData)
                correct = [guesses[i] == validationLabels[i] for i in range(len(validationLabels))].count(True)
                maxvalue[k] = correct
            self.k = maxvalue.argMax()

        self.temp_1.clear()
        self.temp_2.clear()
        self.labels.clear()
        self.dict.clear()
        self.justTrain(trainingData, trainingLabels)

    def classify(self, testData):
        """
        Classify the data based on the posterior distribution over labels.

        DO NOT modify this method.
        """
        guesses = []
        self.posteriors = []  # Log posteriors are stored for later data analysis (autograder).
        for datum in testData:
            posterior = self.calculateLogJointProbabilities(datum)
            guesses.append(posterior.argMax())
            self.posteriors.append(posterior)
        return guesses

    def calculateLogJointProbabilities(self, datum):
        """
        Returns the log-joint distribution over legal labels and the datum.
        Each log-probability should be stored in the log-joint counter, e.g.
        logJoint[3] = <Estimate of log( P(Label = 3, datum) )>
        """
        logJoint = util.Counter()

        "*** YOUR CODE HERE ***"

        for label in self.legalLabels:
            p = math.log(self.temp_1[label])
            q = 0
            for pixel_location in datum:
                value = datum[pixel_location]
                q += math.log(self.temp_2[label][pixel_location][value])
            logJoint[label] = p + q

        return logJoint

    def findHighOddsFeatures(self, label1, label2):
        """
        Returns the 100 best features for the odds ratio:
                P(feature=1 | label1)/P(feature=1 | label2)
        """
        featuresOdds = []

        "*** YOUR CODE HERE ***"
        if label1 not in self.dict or label2 not in self.dict:
            return featuresOdds

        pixel_odds = []
        for pixel_location in self.features:
            ratio = self.temp_2[label1][pixel_location][1] / self.temp_2[label2][pixel_location][1]
            t = (pixel_location, ratio)
            pixel_odds.append(t)
        pixel_odds.sort(key=lambda x: x[1], reverse=True)
        for feature in pixel_odds[0:100]:
            featuresOdds.append(feature[0])
        return featuresOdds