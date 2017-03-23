import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

class Player {
	static final int MIN_FACT = 7;
	static final int MAX_FACT = 15;
	static final int MIN_LINK = MIN_FACT * (MIN_FACT - 1) / 2;
	static final int MAX_LINK = MAX_FACT * (MAX_FACT - 1) / 2;
	static final int MIN_INIT_CYB = 15;
	static final int MAX_INIT_CYB = 30;
	static final int MIN_MOVING_TIME = 1;
	static final int MAX_MOVING_TIME = 20;
	static final int MIN_PROD_CYB = 0;
	static final int MAX_PROD_CYB = 3;
	static final String CYB = "TROOP";
	static final String FACT = "FACTORY";
	static final String BOMB = "BOMB";
	static boolean launchBomb = false;
	static int bombCounter = 2;
	static int stopLaunching = 5;

	public static void main(String args[]) {

		Univ oldMap = new Univ();

		Scanner in = initGame(oldMap);

		
		// game loop
		while (true) {
			Univ newMap = new Univ(oldMap);

			initTurn(in, newMap);

			// System.err.println("My " + newUniv.myArmy);
			// System.err.println("Opp" + newUniv.otherArmy);
			// System.err.println("Blk" + newUniv.neutralArmy);
			
			Decision dec = null;
			try {
				dec = playTurn(oldMap, newMap);
			} catch (CannotDecideException e) {
				System.err.println("CANNOT DECIDE: " + e);
			}

			dec.sendAll();
			System.out.println("WAIT");

			oldMap = newMap;
		}
	}

	static Scanner initGame(Univ oldUniv) {
		Scanner in = new Scanner(System.in);
		int factoryCount = in.nextInt();
		oldUniv.numFact = factoryCount;
		int linkCount = in.nextInt();
		for (int i = 0; i < linkCount; i++) {
			int factory1 = in.nextInt();
			addFactIfNotExist(oldUniv, factory1);
			int factory2 = in.nextInt();
			addFactIfNotExist(oldUniv, factory2);
			int distance = in.nextInt();
			oldUniv.setLink(factory1, factory2, distance);
		}
		return in;
	}

	static void addFactIfNotExist(Univ oldUniv, int factId) {
		oldUniv.gameFactoriesById.putIfAbsent(factId, new Fact(factId));
	}

	static void initTurn(Scanner in, Univ newUniv) {
		Player.stopLaunching--;
		int entityCount = in.nextInt();
		for (int i = 0; i < entityCount; i++) {
			int entityId = in.nextInt();
			String entityType = in.next();
			int arg1 = in.nextInt();
			int arg2 = in.nextInt();
			int arg3 = in.nextInt();
			int arg4 = in.nextInt();
			int arg5 = in.nextInt();
			switch (entityType) {
			case Player.FACT:
				initEntityFact(newUniv, entityId, arg1, arg2, arg3, arg4);
				break;
			case Player.CYB:
				initEntityTroop(newUniv, entityId, arg1, arg2, arg3, arg4, arg5);
				break;
			case Player.BOMB:
				initEntityBomb(newUniv, entityId, arg1, arg2, arg3, arg4);
				break;
			default:
				System.err.println("Entity type not recognized!");
				break;
			}
		}
		analyzeFactories(newUniv);

		sortMotherFacts(newUniv.myArmy);
		sortMotherFacts(newUniv.otherArmy);
		// newUniv.myArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));
		// newUniv.otherArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));

		checkOpportunitiesOnFactories(newUniv.myArmy);
		checkOpportunitiesOnFactories(newUniv.otherArmy);
		newUniv.myArmy.factories.forEach(fact -> {
			if (fact.opportunity != null) {
				if (fact.opportunity.isGoodOmen) {
					System.err.println(fact.base.id + " has "
							+ fact.opportunity.units + " for "
							+ fact.opportunity.distance);
				} else {
					System.err.println(fact.base.id + " needs "
							+ fact.opportunity.units + " in "
							+ fact.opportunity.distance);
				}
			}
		});
		
		for (Fact source : newUniv.myArmy.motherFacts) {
			sortTargetFacts(newUniv.myArmy, new TargetComparator(newUniv,
					source));
			sortTargetFacts(newUniv.otherArmy, new TargetComparator(newUniv,
					source));
			sortTargetFacts(newUniv.otherArmy, newUniv.neutralArmy,
					new TargetComparator(newUniv, source));
		}
	}

	static void initEntityFact(Univ newUniv, int id, int owner, int residents,
			int prod, int turnsLeft) {
		Fact fact = newUniv.gameFactoriesById.get(id);
		fact.owner = owner;
		switch (owner) {
		case -1:
			newUniv.otherArmy.factories.add(fact);
			newUniv.otherArmy.totalCyb += residents;
			newUniv.otherArmy.totalProd += prod;
			break;
		case 0:
			newUniv.neutralArmy.factories.add(fact);
			newUniv.neutralArmy.totalCyb += residents;
			newUniv.neutralArmy.totalProd += prod;
			break;
		case 1:
			newUniv.myArmy.factories.add(fact);
			newUniv.myArmy.totalCyb += residents;
			newUniv.myArmy.totalProd += prod;
			break;
		}
		fact.residents = residents;
		fact.intermediateResidents = residents;
		fact.prod = prod;
		newUniv.gameFactoriesProductionById.put(id, prod);
	}

	static void initEntityTroop(Univ newUniv, int id, int owner, int sourceId,
			int targetId, int cyb, int turnsLeft) {
		switch (owner) {
		case -1:
			newUniv.otherArmy.totalCyb += cyb;
			break;
		case 1:
			newUniv.myArmy.totalCyb += cyb;
			break;
		}
		Fact targetFact = newUniv.gameFactoriesById.get(targetId);
		Fact sourceFact = newUniv.gameFactoriesById.get(sourceId);
		sourceFact.moving += cyb;
		targetFact.addArriving(owner, cyb, turnsLeft);
	}

	static void initEntityBomb(Univ newUniv, int id, int arg1, int arg2,
			int arg3, int arg4) {
	}

	static void analyzeFactories(Univ newUniv) {
		newUniv.myArmy.factories.forEach(fact -> fact.setClosest(
				newUniv.gameFactoriesById, -1));
		newUniv.otherArmy.factories.forEach(fact -> fact.setClosest(
				newUniv.gameFactoriesById, 1));
		// newUniv.myArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));
		// newUniv.otherArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));
	}

	static void sortMotherFacts(PlayingArmy army) {
		army.motherFacts = (ArrayList<Fact>) army.factories.clone();
		Collections.sort(army.motherFacts, new EnemyDistanceComparator());
		//System.err.println("MOTHERS " + army.motherFacts);
	}

	static void checkOpportunitiesOnFactories(PlayingArmy army) {
		army.factories.forEach(f -> Player.checkOpportunitiesOnFactory(f));
	}

	static void checkOpportunitiesOnFactory(Fact fact) {
		int arrivingTotal = 0;
		int minResultant = fact.residents;
		int minDistance = Player.MAX_MOVING_TIME;
		for (Entry<Integer, Integer> entry : fact.arriving.entrySet()) {
			int distance = entry.getKey();
			arrivingTotal += entry.getValue();
			int resultingResidents = fact.residents + distance * fact.prod
					+ arrivingTotal;
			if (resultingResidents < 0) {
				fact.opportunity = new Danger(distance, -resultingResidents);
				return;
			}
			if (resultingResidents < minResultant) {
				minResultant = resultingResidents;
				minDistance = distance;
			}
		}
		fact.opportunity = new Safety(minDistance, minResultant);
	}

	static void sortTargetFacts(Army army, TargetComparator comp) {
		ArrayList<Fact> cloneFacts = (ArrayList<Fact>) army.factories.clone();
		army.weakestFacts.put(comp.sourceFact, cloneFacts);
		Collections.sort(cloneFacts, comp);
	}

	static void sortTargetFacts(PlayingArmy otherArmy, Army neutralArmy,
			TargetComparator comp) {
		ArrayList<Fact> cloneFacts = (ArrayList<Fact>) otherArmy.factories
				.clone();
		ArrayList<Fact> cloneFacts2 = (ArrayList<Fact>) neutralArmy.factories
				.clone();
		cloneFacts.addAll(cloneFacts2);
		neutralArmy.weakestFacts.put(comp.sourceFact, cloneFacts);
		Collections.sort(cloneFacts, comp);
	}

	static Decision playTurn(Univ oldUniv, Univ newUniv)
			throws CannotDecideException {
		Decision dec = new Decision();
		dec.merge(defend(oldUniv, newUniv));
		dec.merge(engage(oldUniv, newUniv));
		return dec;
	}

	static Decision defend(Univ oldUniv, Univ newUniv)
			throws CannotDecideException {
		Decision defend = new Decision();
		ArrayList<Fact> endangeredFactories = new ArrayList<>();
		ArrayList<Fact> safeFactories = new ArrayList<>();
		for (Fact fact : newUniv.myArmy.factories) {
			if (fact.opportunity.isGoodOmen) {
				safeFactories.add(fact);
			} else {
				endangeredFactories.add(fact);
			}
		}
		reorganizeForcesToDefend(defend, newUniv, endangeredFactories,
				safeFactories);
		//defend.print();
		return defend;
	}

	static Decision reorganizeForcesToDefend(Decision result, Univ newUniv,
			ArrayList<Fact> toDefend, ArrayList<Fact> toRelocate)
			throws CannotDecideException {
		final String[] def = { "" };
		toDefend.forEach(t -> def[0] += "," + t.base.id);
		final String[] atk = { "" };
		toRelocate.forEach(t -> atk[0] += "," + t.base.id);
		result.add(new Action("Def" + def[0] + " Atk" + atk[0]));

		for (Fact fact : newUniv.myArmy.motherFacts) {
			if (fact.residents >= 10 && fact.prod < 3)
			{
				if (!newUniv.myArmy.weakestFacts.get(fact).isEmpty())
				{
					if (fact.equals(newUniv.myArmy.weakestFacts.get(fact).get(0)))
					{
						result.add(new Action(fact.base.id));
						fact.toMove = 10;
					}
				}
			}
			//System.err.println("DEFEND from " + fact.base.id);
			//System.err.println("DEFEND from candidates" + toRelocate);
			if (toRelocate.contains(fact)) {
				generateDefMove(result, fact, toDefend);
			}
		}

		return result;
	}

	static Decision generateDefMove(Decision result, Fact source,
			ArrayList<Fact> dests) throws CannotDecideException {
		if (source.opportunity.isGoodOmen) {
			for (Fact dest : dests) {
				if (source.toMove >= source.opportunity.units) {
					return result;
				}
				int troops = Math.min(dest.opportunity.units,
						source.opportunity.units - source.toMove);
				result.add(new Action(source.base.id, dest.base.id, troops));
				source.toMove += troops;
			}
		}
		return result;
	}

	static Decision engage(Univ oldUniv, Univ newUniv)
			throws CannotDecideException {
		Decision atack = new Decision();

		newUniv.myArmy.printTargets();
		newUniv.otherArmy.printTargets();
		newUniv.neutralArmy.printTargets();
		
		System.err.println("ME HAVE "+ newUniv.myArmy.totalCyb);
		System.err.println("OTHER HAVE "+ newUniv.otherArmy.totalCyb);
		if (newUniv.myArmy.factories.size() >= newUniv.gameFactoriesById.size()/2 
				&& newUniv.myArmy.totalCyb <= newUniv.otherArmy.totalCyb * 3 / 2
				&& newUniv.myArmy.totalProd < newUniv.myArmy.factories.size() * Player.MAX_PROD_CYB)
		{
			System.err.println("DEFEND NOW!!!");
			for (Fact fact : newUniv.myArmy.factories)
			{
				if (fact.opportunity.isGoodOmen)
				{
					redistribute(atack, fact);
				}
			}
			return atack;
		}
		
		for (Fact source : newUniv.myArmy.motherFacts) {
			if (!newUniv.neutralArmy.weakestFacts.get(source).isEmpty()) {
				for (int i = 0; i < newUniv.neutralArmy.weakestFacts
						.get(source).size(); i++) {
					if (source.opportunity.isGoodOmen
							&& source.toMove < source.opportunity.units) {
						generateAtkMove(atack, source,
								newUniv.neutralArmy.weakestFacts.get(source)
										.get(i));
					}
				}

			}
		}
		for (Fact source : newUniv.myArmy.motherFacts) {
			if (!newUniv.otherArmy.weakestFacts.get(source).isEmpty()) {
				for (int i = 0; i < newUniv.otherArmy.weakestFacts.get(source)
						.size(); i++) {
					if (source.opportunity.isGoodOmen
							&& source.toMove < source.opportunity.units) {
						generateAtkMove(
								atack,
								source,
								newUniv.otherArmy.weakestFacts.get(source).get(
										i));
					}
				}
			}
		}
		//atack.print();
		return atack;
	}
	
	static void redistribute(Decision atack, Fact source)
	{
		System.err.println("redistrib for "+source);
		if (source.toMove >= source.opportunity.units) {
			return ;
		}
		if (source.opportunity.units >= 10)
		{
			atack.add(new Action(source.base.id));
			source.toMove += 10;
		}
		if (source.prod < Player.MAX_PROD_CYB)
		{
			return;
					
		}
		int troops = source.opportunity.units - source.toMove;
		if (troops > 0)
		{
			Set<Integer> ids = new HashSet<>();
			Iterator<Set<Integer>> it = source.base.neighboursByDistance.values().iterator();
			for (int i = 0;i<2 && it.hasNext();i++)
			{
				ids.addAll(it.next());
			}
			System.err.println("IDS " +ids);
			for (int id : ids)
			{
				int division = troops/ids.size();
				atack.add(new Action(source.base.id, id, division));
				source.toMove += division;
			}	
		}
	}

	static Decision generateAtkMove(Decision result, Fact source, Fact dest)
			throws CannotDecideException {
		if (source.opportunity.isGoodOmen) {
			if (dest.owner == 0) {
				int troops = Math.min(dest.residents + 1,
						source.opportunity.units - source.toMove);
				result.add(new Action(source.base.id, dest.base.id, troops));
				source.toMove += troops;
			} else if (dest.owner == -1) {
				if (dest.opportunity.isGoodOmen) {
					int troops = Math.min(dest.opportunity.units + 1,
							source.opportunity.units - source.toMove);
					result.add(new Action(source.base.id, dest.base.id, troops));
					source.toMove += troops;
				}
			}
		}
		return result;
	}

	static Decision reorganizeForces(Decision result, ArrayList<Fact> toDefend,
			ArrayList<Fact> toRelocate) throws CannotDecideException {
		return result;
	}
}

class Univ {
	PlayingArmy myArmy = new PlayingArmy(1);
	PlayingArmy otherArmy = new PlayingArmy(-1);
	Army neutralArmy = new Army(0);
	HashMap<Integer, Fact> gameFactoriesById = new HashMap<>(Player.MAX_FACT);
	HashMap<Integer, Integer> gameFactoriesProductionById = new HashMap<>(
			Player.MAX_FACT);
	int numFact;

	Univ() {
	}

	Univ(Univ oldUniv) {
		this.numFact = oldUniv.numFact;

		for (Entry<Integer, Fact> entry : oldUniv.gameFactoriesById.entrySet()) {
			// clear up Factories
			this.gameFactoriesById.put(entry.getKey(),
					new Fact(entry.getValue()));
		}
	}

	void setLink(int fact1ID, int fact2ID, int distance) {
		Fact fact1 = gameFactoriesById.get(fact1ID);
		Fact fact2 = gameFactoriesById.get(fact2ID);
		link1To2(fact1, fact2, distance);
		link1To2(fact2, fact1, distance);
	}

	void link1To2(Fact fact1, Fact fact2, int distance) {
		fact1.base.neighboursByDistance.putIfAbsent(distance, new HashSet<>());
		fact1.base.neighboursByDistance.get(distance).add(fact2.base.id);
	}

	int findDistanceFromSource(Fact sourceFact, int id) {
		HashMap<Integer, Set<Integer>> neighboursByDistance = sourceFact.base.neighboursByDistance;
		for (Entry<Integer, Set<Integer>> entry : neighboursByDistance
				.entrySet()) {
			if (entry.getValue().contains(id))
				return entry.getKey();
		}
		return -1;
	}

	public void print() {
		System.err.print(numFact + " : ");
		for (Entry<Integer, Fact> entry : gameFactoriesById.entrySet()) {
			System.err.print("[FACT" + entry.getKey() + ",");
			entry.getValue().print();
			System.err.print("PROD "
					+ gameFactoriesProductionById.get(entry.getKey()) + "]");
		}
	}

	public void printArmies() {
		myArmy.print();
		otherArmy.print();
		neutralArmy.print();
	}

	public void printEnemies() {
		myArmy.factories.forEach(f -> {
			System.err.println("id " + f.base.id);
			System.err.println("closest " + f.closestEnemies);
		});
	}
}

class Army {
	int owner;
	int totalCyb;
	int totalProd;
	HashMap<Fact, ArrayList<Fact>> weakestFacts = new HashMap<>();
	ArrayList<Fact> factories = new ArrayList<>(Player.MAX_FACT);

	Army(int owner) {
		this.owner = owner;
	}

	public void print() {
		System.err.println(owner + " ");
		System.err.println(factories);
	}

	public void printTargets() {
		System.err.println("owner " + owner);
		System.err.println(weakestFacts);
	}
}

class PlayingArmy extends Army {
	ArrayList<Fact> motherFacts;

	PlayingArmy(int owner) {
		super(owner);
	}

	public void print() {
		super.print();
		System.err.println("M" + motherFacts);
		System.err.println("w" + weakestFacts);
	}
}

class FactGroup {
	Integer distance = -1;
	Set<Integer> ids = new HashSet<Integer>();

	void add(int distance, int id) {
		this.distance = distance;
		ids.add(id);
	}

	public String toString() {
		return "" + distance + " : " + ids;
	}
}

class Opportunity {
	Boolean isGoodOmen;
	int distance;
	int units;

	Opportunity() {
		isGoodOmen = null;
	}

	Opportunity(boolean isGoodOmen, int distance, int units) {
		this.isGoodOmen = isGoodOmen;
		this.distance = distance;
		this.units = units;
	}
}

class Danger extends Opportunity {
	Danger(int turnsLeftBeforeInvasion, int unitsNeeded) {
		super(false, turnsLeftBeforeInvasion, unitsNeeded);
	}
}

class Safety extends Opportunity {
	Safety(int turnsLeftBeforeInvasion, int unitsNeeded) {
		super(true, turnsLeftBeforeInvasion, unitsNeeded);
	}
}

class BaseFact {
	int id;
	HashMap<Integer, Set<Integer>> neighboursByDistance = new HashMap<>();

	BaseFact(int id) {
		this.id = id;
	}

	public String print() {
		return "id" + id + neighboursByDistance;
	}
}

class Fact {
	final BaseFact base;
	int owner = -2;
	Integer residents = 0;
	Integer intermediateResidents = 0;
	Integer prod = 0;
	Integer moving = 0;
	int toMove = 0;
	HashMap<Integer, Integer> arriving = new HashMap<>(Player.MAX_MOVING_TIME);
	FactGroup closestEnemies = new FactGroup();
	Opportunity opportunity = null;

	Fact(int id) {
		base = new BaseFact(id);
	}

	// clone base
	Fact(Fact otherFact) {
		this.base = new BaseFact(otherFact.base.id);
		this.base.neighboursByDistance = otherFact.base.neighboursByDistance;
	}

	void addArriving(int owner, int arrivings, int turnsLeft) {
		arriving.putIfAbsent(turnsLeft, 0);
		if (this.owner == owner) {
			arriving.put(turnsLeft, arriving.get(turnsLeft) + arrivings);
		} else {
			arriving.put(turnsLeft, arriving.get(turnsLeft) - arrivings);
		}
	}

	public void setClosest(HashMap<Integer, Fact> facts, int owner) {
		boolean adding = false;
		for (Entry<Integer, Set<Integer>> factGroup : base.neighboursByDistance
				.entrySet()) {
			if (adding) {
				return;
			}
			for (Integer id : factGroup.getValue()) {
				Fact scannedFact = facts.get(id);
				if (scannedFact.owner == owner) {
					int dist = factGroup.getKey();
					closestEnemies.add(dist, id);
					if (!adding && this.owner == -1 && this.prod ==3 && Player.bombCounter > 0 && Player.stopLaunching > 0)
					{
						new Action(id, this.base.id).send();
						Player.stopLaunching = 5;
						Player.bombCounter--;
					}
					adding = true;
				}
			}
		}
	}

	public void print() {
		System.err.println("id" + base.id + "cyb" + residents + "prod" + prod);
	}

	public void printFull() {
		System.err.println(base.toString() + "cyb" + residents + "prod" + prod
				+ "" + moving + arriving + base.neighboursByDistance);
	}

	public String toString() {
		return "id" + base.id + "cyb" + residents + "prod" + prod;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null)
			return false;
		return base.id == ((Fact) obj).base.id;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}
}

class Action {
	BASE base;
	String source;
	String dest;
	int qty;

	Action() {
		base = BASE.WAIT;
	}

	Action(int id) {
		base = BASE.INC;
		qty = id;
	}
	
	Action(String msg) {
		base = BASE.MSG;
		source = msg;
	}

	Action(Integer source, Integer dest) {
		base = BASE.BOMB;
		this.source = source.toString();
		this.dest = dest.toString();
	}

	Action(Integer source, Integer dest, int qty) {
		base = BASE.MOVE;
		this.source = source.toString();
		this.dest = dest.toString();
		this.qty = qty;
	}

	void send() {
		System.out.print(toString());
	}

	public String toString() {
		switch (base) {
		case WAIT:
			return "WAIT;";
		case MOVE:
			return "MOVE " + source + " " + dest + " " + qty + ";";
		case BOMB:
			return "BOMB " + source + " " + dest + ";";
		case MSG:
			return "MSG " + source + ";";
		case INC:
			return "INC " + qty + ";";	
		default:
			return "";
		}
	}

	void print() {
		System.err.println(toString());
	}

	enum BASE {
		WAIT, MOVE, BOMB, MSG, INC
	}
}

class Decision {
	ArrayList<Action> actions = new ArrayList<>();

	Decision add(Action a) {
		actions.add(a);
		return this;
	}

	void sendAll() {
		if (actions.isEmpty()) {
			actions.add(new Action());
		}
		actions.forEach(Action::send);
	}

	void print() {
		if (actions.isEmpty()) {
			System.err.println("EMPTY");
		}
		actions.forEach(Action::print);
	}

	void merge(Decision d) {
		actions.addAll(d.actions);
	}
}

class TargetComparator implements Comparator<Fact> {
	Univ univ;
	Fact sourceFact;

	TargetComparator(Univ univ, Fact sourceFact) {
		this.univ = univ;
		this.sourceFact = sourceFact;
	}

	@Override
	public int compare(Fact fact1, Fact fact2) {
		int id1 = fact1.base.id;
		int id2 = fact2.base.id;
		int d1 = univ.findDistanceFromSource(sourceFact, id1);
		int d2 = univ.findDistanceFromSource(sourceFact, id2);
		int res1 = fact1.residents;
		int res2 = fact2.residents;
		int prod1 = fact1.prod;
		int prod2 = fact2.prod;

		int prodTurns1 = Player.MAX_MOVING_TIME / 2 - d1;
		int prodTurns2 = Player.MAX_MOVING_TIME / 2 - d2;

		Integer profit1 = prod1 * (prodTurns1 < 1 ? 0 : prodTurns1) - res1;
		Integer profit2 = prod2 * (prodTurns2 < 1 ? 0 : prodTurns2) - res2;

		return profit2.compareTo(profit1);
	}
}

class ResidentComparator implements Comparator<Fact> {
	@Override
	public int compare(Fact o1, Fact o2) {
		return o2.residents.compareTo(o1.residents);
	}
}

class EnemyDistanceComparator implements Comparator<Fact> {
	@Override
	public int compare(Fact o1, Fact o2) {
		int res = o1.closestEnemies.distance
				.compareTo(o2.closestEnemies.distance);
		if (res != 0) {
			return res;
		}
		return o2.prod.compareTo(o1.prod);
	}
}

class CannotDecideException extends Exception {
}
