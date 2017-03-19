import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
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

    // Write an action using System.out.println()
    // To debug: System.err.println("Debug messages...");
    // Any valid action, such as "WAIT" or "MOVE source destination cyborgs"
    public static void main(String args[])
    {

        Univ oldMap = new Univ();

        Scanner in = initGame(oldMap);

        // game loop
        while (true)
        {
            Univ newMap = new Univ(oldMap);

            initTurn(in, newMap);

            System.out.println(engage(oldMap, newMap));

            oldMap = newMap;
        }
    }

    static Scanner initGame(Univ oldUniv)
    {
        Scanner in = new Scanner(System.in);
        int factoryCount = in.nextInt(); // the number of factories
        oldUniv.numFact = factoryCount;
        int linkCount = in.nextInt(); // the number of links between factories
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
                    initEntityFact(newUniv, entityId, arg1, arg2, arg3);
                    break;
                case Player.CYB:
                    initEntityTroop(newUniv, entityId, arg1, arg2, arg3, arg4, arg5);
                    break;
                default:
                    System.err.println("Entity type not recognized!");
                    break;
            }
        }
        findMotherFact(newUniv.myArmy);
        findMotherFact(newUniv.otherArmy);
        findTargetFact(newUniv.myArmy, new ProductionComparator(newUniv));
        findTargetFact(newUniv.otherArmy, new ProductionComparator(newUniv));
        findTargetFact(newUniv.neutralArmy, new ProductionComparator(newUniv));
    }

    static void initEntityFact(Univ newUniv, int id, int owner, int residents, int prod)
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

    static void initEntityTroop(Univ newUniv, int id, int arg1, int arg2, int arg3, int arg4, int arg5)
    {
    }

    static void findMotherFact(PlayingArmy army)
    {
        int id = 0;
        int max = 0;
        for (Fact ownFact : army.factories)
        {
            if (ownFact.residents > max)
            {
                max = ownFact.residents;
                id = ownFact.base.id;
            }
        }
        army.motherFact = id;
    }

    static void findTargetFact(Army army, Comparator<Fact> comp)
    {
        if (army.owner == 0)
        {
            army.print();
        }
        ArrayList<Fact> sortedByProd = (ArrayList<Fact>) army.factories.clone();
        if (sortedByProd.isEmpty())
        {
        	army.weakestFact = null;
        	return;
        }
        Collections.sort(sortedByProd, comp);
        army.weakestFact = sortedByProd.get(0).base.id;
        
        if (army.owner == 0)
        {
            System.err.println("weak" + army.weakestFact);
        }
    }

    static String engage(Univ oldUniv, Univ newUniv)
    {
         //       System.err.println("My " + newUniv.myArmy);
         //       System.err.println("Opp" + newUniv.otherArmy);
         //       System.err.println("Blk" + newUniv.neutralArmy);

        Fact otherTarget = newUniv.gameFactoriesById.get(newUniv.otherArmy.weakestFact);
        Fact neutrTarget = newUniv.gameFactoriesById.get(newUniv.neutralArmy.weakestFact);
        
        if (newUniv.neutralArmy.factories.isEmpty() || otherTarget.prod > neutrTarget.prod)
        {
        	if (newUniv.otherArmy.weakestFact == null || newUniv.myArmy.motherFact == newUniv.otherArmy.weakestFact)
            {
               return "WAIT";
            }
            else
            {
            	int moving = newUniv.gameFactoriesById.get(newUniv.myArmy.motherFact).residents - 3;
            	if(moving <= 0)
            		return "WAIT";
               return "MOVE " + newUniv.myArmy.motherFact + " " + newUniv.otherArmy.weakestFact + " " + moving;
            }         
        }
        else
        {
            if (newUniv.myArmy.motherFact == newUniv.neutralArmy.weakestFact)
            {
                return "WAIT";
            }
            else
            {
                return "MOVE " + newUniv.myArmy.motherFact + " " + newUniv.neutralArmy.weakestFact + " " + (newUniv.gameFactoriesById.get(newUniv.myArmy.motherFact).residents -3);
            }
        }
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
}

class Army
{
    int owner;
    Integer weakestFact;
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
    int motherFact;

    PlayingArmy(int owner)
    {
        super(owner);
    }

    public void print()
    {
    	super.print();
        System.err.println("M" + motherFact + "w" + weakestFact);
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
    int residents = 0;
    Integer prod = 0;
    ArrayList<Integer> moving = new ArrayList<>(Player.MAX_MOVING_TIME);
    ArrayList<Integer> arriving = new ArrayList<>(Player.MAX_MOVING_TIME);

    Fact(int id)
    {
        base = new BaseFact(id);
    }

    //clone
    Fact(Fact otherFact)
    {
        this.base = new BaseFact(otherFact.base.id);
        this.base.neighboursByDistance = otherFact.base.neighboursByDistance;
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
		switch (base)
		{
			case WAIT :
				System.out.print("WAIT");
				break;
			case MOVE :
				System.out.print("MOVE " + source + " " + dest + " " + qty);
				break;
			case BOMB :
				System.out.print("BOMB " + source + " " + dest);
				break;
		}
	}
}

class Decision
{
	ArrayList<Action> actions = new ArrayList<>();
	
	void sendAll()
	{
		actions.forEach(Action::send);
	}
}

class ProductionComparator implements Comparator<Fact>
{
	HashMap<Integer, Set<Integer>> neighboursByDistance;
	ProductionComparator(Univ univ)
	{
		this.neighboursByDistance = univ.gameFactoriesById.get(univ.myArmy.motherFact).base.neighboursByDistance;
	}
	
	@Override
	public int compare(Fact fact1, Fact fact2) {
		int id1 = fact1.base.id;
		int id2 = fact2.base.id;
		int d1 = findDistance(id1);
		int d2 = findDistance(id2);
		int res1 = fact1.residents;
		int res2 = fact2.residents;
		int prod1 = fact1.prod;
		int prod2 = fact2.prod;
		
		int prodTurns1 = 5-d1;
		int prodTurns2 = 5-d2;
		
		Integer profit1 = prod1 * (prodTurns1 < 1 ? 0 : prodTurns1) - res1;
		Integer profit2 = prod2 * (prodTurns2 < 1 ? 0 : prodTurns2) - res2;
		
		return profit2.compareTo(profit1);
	}
	
	private int findDistance(int id)
	{
		for(Entry<Integer, Set<Integer>> entry : neighboursByDistance.entrySet())
		{
			if (entry.getValue().contains(id))
				return entry.getKey();
		}
		return -1;
	}
}
