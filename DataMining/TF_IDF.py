__author__ = 'clu10'
import math
import string
import os
import cPickle as pickle
import sys
import operator
from stop_words import *


class article:
    def __init__(self, category, text, name):
        self.category = category
        self.text = text
        self.name = name
        # at first it is TF, then it becomes TF*IDF
        self.dictionary = {}

    def computeTF(self):
        for word in self.text.split():
            word = word.lower()
            word = word.strip(string.punctuation)
            if word in stop_words:
                continue
            if word in self.dictionary:
                self.dictionary[word] += 1
            else:
                self.dictionary[word] = 1

    def computeTFIDF(self, DF, corpus):
        for word in self.dictionary:
            if word not in DF or DF[word] == 0:
                DF[word] = 1
            self.dictionary[word] *= math.log(corpus / DF[word])


def computeDocumentFrequency(directory):
    articles = []
    for root, sub, files in os.walk(directory):
        category = root[len(directory):]
        for filename in files:
            with open(os.path.join(root, filename), "r") as f:
                art = article(category, f.read(), filename)
                art.computeTF()
                articles.append(art)
    DF = {}
    for art in articles:
        for word in art.dictionary:
            if word in DF:
                DF[word] += 1
            else:
                DF[word] = 1
    return articles, DF


def computeTFIDFCategory(directory):
    articles, DF = computeDocumentFrequency(directory)
    TF_IDF = {}
    corpus = len(articles)
    for art in articles:
        art.computeTFIDF(DF, corpus)
        for word in art.dictionary:
            if word in TF_IDF:
                TF_IDF[word] += art.dictionary[word]
            else:
                TF_IDF[word] = art.dictionary[word]

    TF_IDF = dict(sorted(TF_IDF.iteritems(), key=operator.itemgetter(1), reverse=True)[:1000])
    entry = {"articles": articles, "TFIDFCategory": TF_IDF}
    return pickle.dumps(entry)


def classify(art, directory):
    cos = {}
    for root, sub, files in os.walk(directory):
        for k in sub:
            categoryTFIDF = pickle.loads(computeTFIDFCategory(os.path.join(root, k)))["TFIDFCategory"]
            t = cosineSimilarity(art.dictionary, categoryTFIDF)
            cos[k] = t
    return max(cos.iteritems(), key=operator.itemgetter(1))[0]


def cosineSimilarity(dictionary1, dictionary2):
    a = b = c = 0
    for word in dictionary1:
        if word in dictionary2:
            a += dictionary1[word] * dictionary2[word]

    for word in dictionary1:
        b += math.pow(dictionary1[word], 2)
    for word in dictionary2:
        c += math.pow(dictionary2[word], 2)
    return a / (math.sqrt(b) * math.sqrt(c))


def hCluster(directory):
    TF_IDF = {}
    S = []
    for root, sub, files in os.walk(directory):
        for k in sub:
            TF_IDF[k] = pickle.loads(computeTFIDFCategory(os.path.join(root, k)))["TFIDFCategory"]
            S.append(k)

    new = ""
    while len(S) > 1:
        cos = -1
        a = b = ""
        for i in S:
            for j in S:
                if i != j:
                    t = cosineSimilarity(TF_IDF[i], TF_IDF[j])
                    if t >= cos:
                        a, b = i, j
                        cos = t
        S.remove(a)
        S.remove(b)
        new = "{ " + a + " U " + b + " }"
        S.append(new)
        for word in TF_IDF[a]:
            if word in TF_IDF[b]:
                TF_IDF[b][word] = TF_IDF[a][word] + TF_IDF[b][word]
            else:
                TF_IDF[b][word] = TF_IDF[a][word]
        TF_IDF[new] = TF_IDF[b]
        del TF_IDF[a]
        del TF_IDF[b]
    print new

if __name__ == '__main__':
    directory = sys.argv[-1]
    hCluster(directory)