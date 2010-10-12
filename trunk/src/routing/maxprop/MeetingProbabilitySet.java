/* 
 * Copyright 2008 TKK/ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package routing.maxprop;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import core.SimClock;

/**
 * Class for storing and manipulating the meeting probabilities for the MaxProp
 * router module.
 */
public class MeetingProbabilitySet {
	/** meeting probabilities (probability that the next node one meets is X) */
	private Map<Integer, Double> probs;
	/** the time when this MPS was last updated */
	private double lastUpdateTime;

	/**
	 * Constructor. Creates a probability set with empty node-probability
	 * mapping.
	 */
	public MeetingProbabilitySet() {
		this.probs = new HashMap<Integer, Double>();
		this.lastUpdateTime = 0;
	}

	/**
	 * Constructor. Creates a probability set with equal probability for all the
	 * given node indexes.
	 */
	public MeetingProbabilitySet(List<Integer> initiallyKnownNodes) {
		this();
		double prob = 1.0 / initiallyKnownNodes.size();
		for (Integer i : initiallyKnownNodes) {
			this.probs.put(i, prob);
		}
	}

	/**
	 * Updates meeting probability for the given node index.
	 * 
	 * <PRE>
	 * P(b) = P(b)_old + 1
	 * Normalize{P}
	 * </PRE>
	 * 
	 * I.e., The probability of the given node index is increased by one and
	 * then all the probabilities are normalized so that their sum equals to 1.
	 * 
	 * @param index
	 *            The node index to update the probability for
	 */
	public void updateMeetingProbFor(Integer index) {
		this.lastUpdateTime = SimClock.getTime();

		if (probs.size() == 0) { // first entry
			probs.put(index, 1.0);
			return;
		}

		double newValue = getProbFor(index) + 1;
		probs.put(index, newValue);

		/*
		 * now the sum of all entries is 2; normalize to one by dividing all the
		 * entries by 2
		 */
		for (Map.Entry<Integer, Double> entry : probs.entrySet()) {
			entry.setValue(entry.getValue() / 2.0);
		}
	}

	/**
	 * Returns the current delivery probability value for the given node index
	 * 
	 * @param index
	 *            The index of the node to look the P for
	 * @return the current delivery probability value
	 */
	public double getProbFor(Integer index) {
		if (probs.containsKey(index)) {
			return probs.get(index);
		} else {
			/* the node with the given index has not been met */
			return 0.0;
		}
	}

	/**
	 * Returns a reference to the probability map of this probability set
	 * 
	 * @return a reference to the probability map of this probability set
	 */
	public Map<Integer, Double> getAllProbs() {
		return this.probs;
	}

	/**
	 * Returns the time when this probability set was last updated
	 * 
	 * @return the time when this probability set was last updated
	 */
	public double getLastUpdateTime() {
		return this.lastUpdateTime;
	}

	/**
	 * Returns a deep copy of the probability set
	 * 
	 * @return a deep copy of the probability set
	 */
	public MeetingProbabilitySet replicate() {
		MeetingProbabilitySet replicate = new MeetingProbabilitySet();
		// do a deep copy
		for (Map.Entry<Integer, Double> e : probs.entrySet()) {
			replicate.probs.put(e.getKey(), e.getValue().doubleValue());
		}

		replicate.lastUpdateTime = this.lastUpdateTime;
		return replicate;
	}

	/**
	 * Returns a String presentation of the probabilities
	 * 
	 * @return a String presentation of the probabilities
	 */
	public String toString() {
		return "probs: " + this.probs.toString();
	}
}