__author__ = 'clu10'
import sys, math, re
import cPickle as pickle
import readARFF
import random


### takes as input a list of class labels. Returns a float
### indicating the entropy in this data.
def entropy(data):
    possibleLabel = set([item for item in data])
    r = 0.0
    for label in possibleLabel:
        c = [item for item in data].count(label)
        r -= (float(c) / len(data)) * math.log(float(c) / len(data), 2)
    return r


### Compute remainder - this is the amount of entropy left in the data after
### we split on a particular attribute. Let's assume the input data is of
### the form:
###    [(value1, class1), (value2, class2), ..., (valueN, classN)]
def remainder(data):
    possibleValues = set([item[0] for item in data])
    r = 0.0
    for value in possibleValues:
        c = [item[0] for item in data].count(value)
        r += (float(c) / len(data)) * entropy([item[1] for item in data if item[0] == value])
    return r


### selectAttribute: choose the index of the attribute in the current
### dataSet that minimizes the remainder.
### data is in the form [[a1, a2, ..., c1], [b1,b2,...,c2], ... ]
### where the a's are attribute values and the c's are classifications.
### and attributes is a list [a1,a2,...,an] of corresponding attribute values
def selectAttribute(data, attributes):
    temp = 1000
    answer = ""
    for attribute in attributes:
        if attribute is not None:
            index = attributes.index(attribute)
            r = remainder([(d[index], d[-1]) for d in data])
            if r < temp:
                temp = r
                answer = attribute
    return answer


### a TreeNode is an object that has either:
### 1. An attribute to be tested and a set of children; one for each possible
### value of the attribute.
### 2. A value (if it is a leaf in a tree)
class TreeNode:
    def __init__(self, attribute, value):
        self.attribute = attribute
        self.value = value
        self.children = {}

    def __repr__(self):
        if self.attribute:
            return self.attribute
        else:
            return self.value

    ### a node with no children is a leaf
    def isLeaf(self):
        return self.children == {}

    ### return the value for the given data
    ### the input will be:
    ### data - an object to classify - [v1, v2, ..., vn]
    ### attributes - the attribute dictionary
    def classify(self, data, attributes):
        if self.isLeaf():
            return self.value
        else:
            listAttributes = readARFF.getAttrList(attributes)
            value = data[listAttributes.index(self.attribute)]
            #if value not in self.children:
            #if not self.children[value]:
            #if len(self.children[value]) == 0:
            if self.children[value].attribute is None:
                return self.value
            child = self.children[value]
            return child.classify(data, attributes)


### a tree is simply a data structure composed of nodes (of type TreeNode).
### The root of the tree
### is itself a node, so we don't need a separate 'Tree' class. We
### just need a function that takes in a dataSet and our attribute dictionary,
### builds a tree, and returns the root node.
### makeTree is a recursive function. Our base case is that our
### dataset has entropy 0 - no further tests have to be made. There
### are two other degenerate base cases: when there is no more data to
### use, and when we have no data for a particular value. In this case
### we use either default value or majority value.
### The recursive step is to select the attribute that most increases
### the gain and split on that.


### assume: input looks like this:
### dataSet: [[v1, v2, ..., vn, c1], [v1,v2, ..., c2] ... ]
### attributes: [a1,a2,...,an] }
def makeTree(dataSet, aList, attributes, defaultValue):
    if entropy([d[-1] for d in dataSet]) == 0:
        return TreeNode(None, dataSet[0][-1])
    elif len(aList) == 0:
        return TreeNode(None, defaultValue)
    else:
        listAttributes = readARFF.getAttrList(attributes)
        for index, item in enumerate(listAttributes):
            if item not in aList:
                listAttributes[index] = None
        attribute = selectAttribute(dataSet, listAttributes)
        index = listAttributes.index(attribute)
        possibleValue = attributes[index][attribute]
        aList.remove(attribute)
        node = TreeNode(attribute, None)
        for value in possibleValue:
            subSet = [d for d in dataSet if d[index] == value]
            if len(subSet) == 0:
                node.children[value] = TreeNode(None, readARFF.computeZeroR(attributes, dataSet))
            else:
                node.children[value] = makeTree(subSet, aList, attributes, readARFF.computeZeroR(attributes, subSet))
        return node


def printNode(root):
    print root
    if len(root.children) != 0:
        for k in root.children:
            child = root.children[k]
            printNode(child)


if __name__ == '__main__':
    fileName = sys.argv[-1]
    attributes, data = readARFF.readArff(open(fileName))
    listAttributes = readARFF.getAttrList(attributes)
    times = 5
    total = zero = 0
    precision = recall = precisionZero = recallZero = 0
    for i in range(times):
        trainData = random.sample(data, int(len(data) * 0.8))
        defaultValue = readARFF.computeZeroR(attributes, data)
        zeroRValue = readARFF.computeZeroR(attributes, trainData)
        root = makeTree(trainData, listAttributes, attributes, defaultValue)
        #printNode(root)
        TP = tp = 0
        testData = []
        for d in data:
            if d not in trainData:
                testData.append(d)
        for d in testData:
            value = root.classify(d, attributes)
            if value == d[-1]:
                TP += 1
            if zeroRValue == d[-1]:
                tp += 1
        accuracy = float(TP) / len(testData)
        another = float(tp) / len(testData)
        total += accuracy
        zero += another
    print "Decision Tree: " + "{0:.0f}%".format((total / times) * 100)
    print "ZeroR: " + "{0:.0f}%".format((zero / times) * 100)