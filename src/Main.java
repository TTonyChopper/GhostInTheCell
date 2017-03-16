import java.util.ArrayList;
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

//            System.err.print("old: " + oldMap);
//            System.err.print("new: " + newMap);

            engage(oldMap, newMap);

            System.out.println("WAIT");
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
    }

    static void initEntityFact(Univ newUniv, int id, int owner, int residents, int prod)
    {
        Fact fact = newUniv.gameFactoriesById.get(id);
        fact.owner = owner;
        switch (id)
        {
            case -1:
                break;
            case 0:
                break;
            case 1:
                break;
        }
        fact.residents = residents;
        fact.prod = prod;
        newUniv.gameFactoriesProductionById.put(id, prod);
    }

    static void initEntityTroop(Univ newUniv, int id, int arg1, int arg2, int arg3, int arg4, int arg5)
    {
    }

    static void engage(Univ oldUniv, Univ newUniv)
    {

    }
}

class Univ
{
    Army myArmy = new PlayingArmy();
    Army otherArmy = new PlayingArmy();
    Army neutralArmy = new Army();
    HashMap<Integer, Fact> gameFactoriesById = new HashMap<>(Player.MAX_FACT);
    HashMap<Integer, Integer> gameFactoriesProductionById = new HashMap<>(Player.MAX_FACT);
    int numFact;

    Univ()
    {
    }

    Univ(Univ oldUniv)
    {
        this.myArmy = oldUniv.myArmy;
        this.otherArmy = oldUniv.otherArmy;
        this.neutralArmy = oldUniv.neutralArmy;
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

    public String toString()
    {
        String result = numFact + " : " ;
        for (Entry<Integer, Fact> entry : gameFactoriesById.entrySet())
        {
            result += "[FACT" + entry.getKey()+ ",";
            result += entry.getValue();
            result += "PROD " + gameFactoriesProductionById.get(entry.getKey());
            result += "]";
        }
        return result;
    }
}

class Army
{
    int owner;
    ArrayList<Fact> factories = new ArrayList<>(Player.MAX_FACT);

    public String toString()
    {
        return owner + " " + factories;
    }
}

class PlayingArmy extends Army
{
    Fact motherFact;

    void setNewMotherFactory(Integer id)
    {
    }

    public String toString()
    {
        return motherFact.toString();
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

    public String toString()
    {
        return "id " + id;
    }
}

class Fact
{
    final BaseFact base;
    int owner;
    int residents = 0;
    int prod = 0;
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

    public String toString()
    {
        return super.toString() + " residents " + residents + " prod " + prod + "" + moving + arriving + base.neighboursByDistance;
    }
}



//TODO
// class SameIdComparator()
