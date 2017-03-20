import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

class Player
{
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

    public static void main(String args[])
    {

        Univ oldMap = new Univ();

        Scanner in = initGame(oldMap);

        // game loop
        while (true)
        {
            Univ newMap = new Univ(oldMap);

            initTurn(in, newMap);

            //System.err.println("My " + newUniv.myArmy);
            //System.err.println("Opp" + newUniv.otherArmy);
            //System.err.println("Blk" + newUniv.neutralArmy);
           
            Decision dec = playTurn(oldMap, newMap);
           
            dec.sendAll();
            System.out.println("WAIT");

            oldMap = newMap;
        }
    }

    static Scanner initGame(Univ oldUniv)
    {
        Scanner in = new Scanner(System.in);
        int factoryCount = in.nextInt();
        oldUniv.numFact = factoryCount;
        int linkCount = in.nextInt();
        for (int i = 0; i < linkCount; i++)
        {
            int factory1 = in.nextInt();
            addFactIfNotExist(oldUniv, factory1);
            int factory2 = in.nextInt();
            addFactIfNotExist(oldUniv, factory2);
            int distance = in.nextInt();
            oldUniv.setLink(factory1, factory2, distance);
        }
        return in;
    }

    static void addFactIfNotExist(Univ oldUniv, int factId)
    {
        oldUniv.gameFactoriesById.putIfAbsent(factId, new Fact(factId));
    }

    static void initTurn(Scanner in, Univ newUniv)
    {
        int entityCount = in.nextInt(); // the number of entities (e.g. factories and troops)
        for (int i = 0; i < entityCount; i++)
        {
            int entityId = in.nextInt();
            String entityType = in.next();
            int arg1 = in.nextInt();
            int arg2 = in.nextInt();
            int arg3 = in.nextInt();
            int arg4 = in.nextInt();
            int arg5 = in.nextInt();
            switch (entityType)
            {
                case Player.FACT:
                    initEntityFact(newUniv, entityId, arg1, arg2, arg3, arg4);
                    break;
                case Player.CYB:
                    initEntityTroop(newUniv, entityId, arg1, arg2, arg3, arg4, arg5);
                    break;
                case Player.BOMB :
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
        //checkEndangeredFactory(newUniv.myArmy);
        //sortTargetFacts(newUniv.myArmy, new TargetComparator(newUniv));
        //sortTargetFacts(newUniv.otherArmy, new TargetComparator(newUniv));
        //sortTargetFacts(newUniv.neutralArmy, new TargetComparator(newUniv));
    }

    static void initEntityFact(Univ newUniv, int id, int owner, int residents, int prod, int turnsLeft)
    {
        Fact fact = newUniv.gameFactoriesById.get(id);
        fact.owner = owner;
        switch (owner)
        {
            case -1:
                newUniv.otherArmy.factories.add(fact);
                break;
            case 0:
                newUniv.neutralArmy.factories.add(fact);
                break;
            case 1:
                newUniv.myArmy.factories.add(fact);
                break;
        }
        fact.residents = residents;
        fact.prod = prod;
        newUniv.gameFactoriesProductionById.put(id, prod);
    }

    static void initEntityTroop(Univ newUniv, int id, int owner, int sourceId, int targetId, int cyb, int turnsLeft)
    {
    	Fact targetFact = newUniv.gameFactoriesById.get(targetId);
    	Fact sourceFact = newUniv.gameFactoriesById.get(sourceId);
    	sourceFact.moving += cyb;
		targetFact.addArriving(owner, cyb, turnsLeft);
    }
    
    static void initEntityBomb(Univ newUniv, int id, int arg1, int arg2, int arg3, int arg4)
    {
    }
    
    static void analyzeFactories(Univ newUniv)
    {
    	newUniv.myArmy.factories.forEach(fact->fact.setClosest(newUniv.gameFactoriesById, -1));
    	newUniv.otherArmy.factories.forEach(fact->fact.setClosest(newUniv.gameFactoriesById, 1));
    	newUniv.myArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));
    	newUniv.otherArmy.factories.forEach(fact->System.err.println(fact.base.id+" "+fact.closestEnemies));
    }
    
    static void checkEndangeredFactory(PlayingArmy army)
    {
    }

    static void sortMotherFacts(PlayingArmy army)
    {
    	army.motherFacts = (ArrayList<Fact>) army.factories.clone();
        Collections.sort(army.motherFacts, new EnemyDistanceComparator());
    }

    static void sortTargetFacts(Army army, Comparator<Fact> comp)
    {
        army.weakestFacts = (ArrayList<Fact>) army.factories.clone();
        Collections.sort(army.weakestFacts, comp);
    }
    
    static Decision playTurn(Univ oldUniv, Univ newUniv)
    {
        Decision dec = new Decision();
    	defend(dec, oldUniv, newUniv);
        engage(dec, oldUniv, newUniv);
        return dec;
    }
    
    static Decision defend(Decision result, Univ oldUniv, Univ newUniv)
    {	
    	return result;
    }

    static Decision engage(Decision result, Univ oldUniv, Univ newUniv)
    {
    
    	return result;
    	/*
        Fact otherTarget = newUniv.gameFactoriesById.get(newUniv.otherArmy.weakestFact);
        Fact neutrTarget = newUniv.gameFactoriesById.get(newUniv.neutralArmy.weakestFact);
        
        if (otherTarget == null)
        {
            return result;
        }
        if (newUniv.neutralArmy.factories.isEmpty() || newUniv.findDistance(otherTarget.base.id) <= newUniv.findDistance(neutrTarget.base.id))
        {
        	if (newUniv.otherArmy.weakestFact == null || newUniv.myArmy.motherFacts.get(0) == newUniv.otherArmy.weakestFact.get(0))
            {
               return result.add(new Action());
            }
            else
            {
            	int moving = newUniv.gameFactoriesById.get(newUniv.myArmy.motherFacts.get(0)).residents - 3;
            	if(moving <= 0)
            		return result;
               return result.add(new Action(newUniv.myArmy.motherFacts.get(0), newUniv.otherArmy.weakestFact.get(0), moving));
            }         
        }
        else
        {
            if (newUniv.myArmy.motherFacts.get(0) == newUniv.neutralArmy.weakestFact.get(0))
            {
            	return result;
            }
            else
            {
            	int moving = newUniv.gameFactoriesById.get(newUniv.myArmy.motherFacts.get(0)).residents - 3;
            	if(moving <= 0)
            		return result;
            	return result.add(new Action(newUniv.myArmy.motherFacts.get(0), newUniv.neutralArmy.weakestFact.get(0), moving));
            }
        }*/
    }
}

class Univ
{
    PlayingArmy myArmy = new PlayingArmy(1);
    PlayingArmy otherArmy = new PlayingArmy(-1);
    Army neutralArmy = new Army(0);
    HashMap<Integer, Fact> gameFactoriesById = new HashMap<>(Player.MAX_FACT);
    HashMap<Integer, Integer> gameFactoriesProductionById = new HashMap<>(Player.MAX_FACT);
    int numFact;

    Univ()
    {
    }

    Univ(Univ oldUniv)
    {
        this.numFact = oldUniv.numFact;

        for (Entry<Integer, Fact> entry : oldUniv.gameFactoriesById.entrySet())
        {
            //clear up Factories
            this.gameFactoriesById.put(entry.getKey(), new Fact(entry.getValue()));
        }
    }

    void setLink(int fact1ID, int fact2ID, int distance)
    {
        Fact fact1 = gameFactoriesById.get(fact1ID);
        Fact fact2 = gameFactoriesById.get(fact2ID);
        link1To2(fact1, fact2, distance);
        link1To2(fact2, fact1, distance);
    }

    void link1To2(Fact fact1, Fact fact2, int distance)
    {
        fact1.base.neighboursByDistance.putIfAbsent(distance, new HashSet<>());
        fact1.base.neighboursByDistance.get(distance).add(fact2.base.id);
    }
    
    int findDistance(int id)
	{
		HashMap<Integer, Set<Integer>> neighboursByDistance = gameFactoriesById.get(myArmy.motherFacts.get(0)).base.neighboursByDistance;
		for(Entry<Integer, Set<Integer>> entry : neighboursByDistance.entrySet())
		{
			if (entry.getValue().contains(id))
				return entry.getKey();
		}
		return -1;
	}

    public void print()
    {
        System.err.print(numFact + " : ");
        for (Entry<Integer, Fact> entry : gameFactoriesById.entrySet())
        {
            System.err.print("[FACT" + entry.getKey() + ",");
            entry.getValue().print();
            System.err.print("PROD " + gameFactoriesProductionById.get(entry.getKey()) + "]");
        }
    }
    
    public void printArmies()
    {
        myArmy.print();
        otherArmy.print();
        neutralArmy.print();
    }
    
    public void printEnemies()
    {
    	myArmy.factories.forEach(f->{System.err.println("id "+f.base.id);System.err.println("closest "+f.closestEnemies);});
    }
}

class Army
{
    int owner;
    ArrayList<Fact> weakestFacts;
    ArrayList<Fact> factories = new ArrayList<>(Player.MAX_FACT);

    Army(int owner)
    {
        this.owner = owner;
    }

    public void print()
    {
        System.err.println(owner + " ");
        System.err.println(factories);
    }
}

class PlayingArmy extends Army
{
	ArrayList<Fact> motherFacts;

    PlayingArmy(int owner)
    {
        super(owner);
    }

    public void print()
    {
    	super.print();
        System.err.println("M" + motherFacts);
        System.err.println("w" + weakestFacts);
    }
}

class FactGroup
{
	Integer distance = -1;
	Set<Integer> ids = new HashSet<Integer>();
	void add(int distance, int id)
	{
		this.distance = distance;
		ids.add(id);
	}
	public String toString()
	{
		return ""+distance+" : "+ids;
	}
}

class Danger
{
	int turnsLeftBeforeInvasion;
	int unitsNeeded;
	Danger(int turns, int unitsNeeded)
	{
		this.turnsLeftBeforeInvasion = turns;
		this.unitsNeeded = unitsNeeded;
	}
}

class BaseFact
{
    int id;
    HashMap<Integer, Set<Integer>> neighboursByDistance = new HashMap<>();

    BaseFact(int id)
    {
        this.id = id;
    }

    public String print()
    {
        return "id" + id + neighboursByDistance;
    }
}

class Fact
{
    final BaseFact base;
    int owner = -2;
    Integer residents = 0;
    Integer prod = 0;
    Integer moving = 0;
    HashMap<Integer, Integer> arriving = new HashMap<>(Player.MAX_MOVING_TIME);
    FactGroup closestEnemies = new FactGroup();
    

    Fact(int id)
    {
        base = new BaseFact(id);
    }

    //clone base
    Fact(Fact otherFact)
    {
        this.base = new BaseFact(otherFact.base.id);
        this.base.neighboursByDistance = otherFact.base.neighboursByDistance;
    }
    
    void addArriving(int owner, int arrivings, int turnsLeft)
    {	
    	if (arriving.get(turnsLeft) == null)
    		arriving.put(turnsLeft, 0);
    	if (this.owner == owner)
    	{
    		arriving.put(turnsLeft, arrivings + arriving.get(turnsLeft));
    	}
    	else
    	{
    		arriving.put(turnsLeft, arrivings - arriving.get(turnsLeft));
    	}
    }
    
    public void setClosest(HashMap<Integer, Fact> facts, int owner)
    {
    	boolean adding = false;
    	for(Entry<Integer, Set<Integer>> factGroup : base.neighboursByDistance.entrySet())
    	{
    		if (adding)
    		{
    			return;
    		}
    		for(Integer id : factGroup.getValue())
    		{
    			Fact scannedFact = facts.get(id);
    			if (scannedFact.owner == owner)
    			{
    				adding = true;
    				closestEnemies.add(factGroup.getKey(), id);
    			}
    		}
    	}
    }

    public void print()
    {
        System.err.println("id" + base.id + "cyb" + residents + "prod" + prod);
    }

    public void printFull()
    {
        System.err.println(base.toString() + "cyb" + residents + "prod" + prod + "" + moving + arriving + base.neighboursByDistance);
    }

    public String toString()
    {
        return "id" + base.id + "cyb" + residents + "prod" + prod;
    }
}

class Action
{
	enum BASE{WAIT, MOVE, BOMB}
	BASE base;
	String source;
	String dest;
	int qty;
	void send()
	{
		System.out.print(toString());
	}
	Action()
	{
		base = BASE.WAIT;
	}
	Action(Integer source, Integer dest)
	{
		base = BASE.BOMB;
		this.source = source.toString();
		this.dest = dest.toString();
	}
	Action(Integer source, Integer dest, int qty)
	{
		base = BASE.MOVE;
		this.source = source.toString();
		this.dest = dest.toString();
		this.qty = qty;
	}
	public String toString()
	{
		switch (base)
		{
			case WAIT :
				return "WAIT;";
			case MOVE :
				return "MOVE " + source + " " + dest + " " + qty +";";
			case BOMB :
				return "BOMB " + source + " " + dest +";";
			default : return "";
		}
	}
	void print()
	{
		System.err.println(toString());
	}
}

class Decision
{
	ArrayList<Action> actions = new ArrayList<>();	
	Decision add(Action a)
	{
		actions.add(a);
		return this;
	}
	
	void sendAll()
	{
		if (actions.isEmpty())
		{
			actions.add(new Action());
		}
		actions.forEach(Action::send);
	}
	
	void print()
	{
		actions.forEach(Action::print);
	}
}

class TargetComparator implements Comparator<Fact>
{
	Univ univ;
	TargetComparator(Univ univ)
	{
	   this.univ = univ;
	}
	
	@Override
	public int compare(Fact fact1, Fact fact2) {
		int id1 = fact1.base.id;
		int id2 = fact2.base.id;  
		int d1 = univ.findDistance(id1);
		int d2 = univ.findDistance(id2);
		int res1 = fact1.residents;
		int res2 = fact2.residents;
		int prod1 = fact1.prod;
		int prod2 = fact2.prod;
		
		int prodTurns1 = Player.MAX_MOVING_TIME-d1;
		int prodTurns2 = Player.MAX_MOVING_TIME-d2;
		
		Integer profit1 = prod1 * (prodTurns1 < 1 ? 0 : prodTurns1) - res1;
		Integer profit2 = prod2 * (prodTurns2 < 1 ? 0 : prodTurns2) - res2;
		
		return profit2.compareTo(profit1);
	}
}

class ResidentComparator implements Comparator<Fact>
{
	@Override
	public int compare(Fact o1, Fact o2) {
		return o2.residents.compareTo(o1.residents);
	}
}

class EnemyDistanceComparator implements Comparator<Fact>
{
	@Override
	public int compare(Fact o1, Fact o2) {
	    int res = o2.closestEnemies.distance.compareTo(o2.closestEnemies.distance) ;
	    if (res != 0)
	    {
	        return res;
	    }
		return o2.prod.compareTo(o1.prod);
	}
}