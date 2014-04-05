package ciceronulus.words;

import java.util.ArrayList;

public class VerbEndActIndFut extends VerbEnding{

	String end;
	String voice;
	String mood;
	String tense;
	String person; 
	String number;


	public VerbEndActIndFut(String end, String voice,String mood, String tense, String person, String number) {
		this.end = end;
		this.voice = voice;
		this.mood = mood;
		this.tense = tense;
		this.person = person;
		this.number = number;

	}
	
public ArrayList<String> addToParse(ArrayList<String> Parse){
		
		Parse.add(end);
		Parse.add(voice);
		Parse.add(mood);
		Parse.add(tense);
		Parse.add(person);
		Parse.add(number);
	
		return Parse;
		
	}
}
