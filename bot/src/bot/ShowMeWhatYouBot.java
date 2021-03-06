/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor
 */
package bot;

import ai.abstraction.AbstractAction;
import ai.abstraction.AbstractionLayerAI;
import ai.abstraction.Harvest;
import ai.abstraction.pathfinding.AStarPathFinding;
import ai.core.AI;
import ai.core.ParameterSpecification;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import rts.*;
import rts.units.Unit;
import rts.units.UnitType;
import rts.units.UnitTypeTable;
import rts.UnitAction;

/**
 *
 * @author newtoto
 */
public class ShowMeWhatYouBot extends AbstractionLayerAI {
	
	Random r = new Random();
	protected UnitTypeTable utt;
	// Static units
	UnitType baseType;
	UnitType barracksType;
	UnitType resourceType;
	// Mobile units
	UnitType workerType;
	UnitType rangedType;
	UnitType lightType;
	UnitType heavyType;
	
    public ShowMeWhatYouBot(UnitTypeTable a_utt) {
    	this(a_utt, new AStarPathFinding());
    }
    
    public ShowMeWhatYouBot(UnitTypeTable utt, AStarPathFinding aStarPathFinding) {
    	super(aStarPathFinding);
    	reset(utt);
    }
    
    public void reset() {
    	super.reset();
    }
    
    public void reset(UnitTypeTable a_utt) {
        utt = a_utt;
        // Add static units to type table
        baseType = utt.getUnitType("Base");
        barracksType = utt.getUnitType("Barracks");
        resourceType = utt.getUnitType("Resource");
        // Add mobile units to type table
        workerType = utt.getUnitType("Worker");
        lightType = utt.getUnitType("Light");
        heavyType = utt.getUnitType("Heavy");
        rangedType = utt.getUnitType("Ranged");
    }
    
    public AI clone() {
        return new ShowMeWhatYouBot(utt);
    }
   
    // This is what gets executed during the game
    public PlayerAction getAction(int playerNumber, GameState gameState) {
    	PhysicalGameState physicalGameState = gameState.getPhysicalGameState();
    	Player player = physicalGameState.getPlayer(playerNumber);
    	
    	//System.out.println(gameInfo.get(baseType));	
    	
    	// __GATHERING AND STORING OF GAME INFO START__
    	// Initialise maps of friendly unit lists
    	Map <String, List<Unit>> friendlyUnits = new HashMap<String, List<Unit>>();
    	// Create unit lists and add to friendlyUnit map
    	// Static units
    	List<Unit> bases = new LinkedList<Unit>();
    	friendlyUnits.put("bases", bases);
    	List<Unit> barracks = new LinkedList<Unit>();
    	friendlyUnits.put("barracks", barracks);
    	List<Unit> resources = new LinkedList<Unit>();
    	friendlyUnits.put("resources", resources);
    	// Mobile units
        List<Unit> workers = new LinkedList<Unit>();
        friendlyUnits.put("workers", workers);
        List<Unit> light = new LinkedList<Unit>();
        friendlyUnits.put("light", light);
        List<Unit> heavy = new LinkedList<Unit>();
        friendlyUnits.put("heavy", heavy);
        List<Unit> ranged = new LinkedList<Unit>();
        friendlyUnits.put("ranged", ranged);
        
        // Initialise maps of enemy unit lists
        Map <String, List<Unit>> enemyUnits = new HashMap<String, List<Unit>>();
        // Create unit lists and add to enemyUnit map
        // Static units
        List<Unit> enemyBases = new LinkedList<Unit>();
        enemyUnits.put("bases", enemyBases);
        List<Unit> enemyBarracks = new LinkedList<Unit>();
        enemyUnits.put("barracks", enemyBarracks);
        // Mobile units
        List<Unit> enemyWorkers = new LinkedList<Unit>();
        enemyUnits.put("workers", enemyWorkers);
        List<Unit> enemyLight = new LinkedList<Unit>();
        enemyUnits.put("light", enemyLight);
        List<Unit> enemyHeavy = new LinkedList<Unit>();
        enemyUnits.put("heavy", enemyHeavy);
        List<Unit> enemyenemyRanged = new LinkedList<Unit>();
        enemyUnits.put("ranged", enemyenemyRanged);
        
        // Populate unit lists
        AddUnitsToLists(friendlyUnits, playerNumber, physicalGameState);
        AddEnemyUnitsToLists(enemyUnits, playerNumber, physicalGameState);
        
        // Create map of friendly unit numbers
    	Map <UnitType, Integer> gameInfo = new HashMap<UnitType, Integer>();
    	// Fill game information using unit lists
    	gameInfo.put(baseType, bases.size());
    	gameInfo.put(barracksType, barracks.size());
    	gameInfo.put(resourceType, resources.size());
    	gameInfo.put(workerType, workers.size());
    	gameInfo.put(lightType, light.size());
    	gameInfo.put(heavyType, heavy.size());
    	gameInfo.put(rangedType, ranged.size());	
    	
    	
    	// __CONTROLLING UNITS START__
    	int resourcesFarmed = player.getResources();
    	
        // Control base
        boolean enoughWorkers = BaseController(bases, gameInfo);
        
        // Prioritise creating enough workers
        // Control barracks (must be done after base)
        if(enoughWorkers)
        {
        	BarracksController(barracks, gameInfo);
        }
        
        if(bases.isEmpty())
        {
        	// Workers attack
        	PrioritiseBases(workers, enemyUnits);
        }
        else
        {
        	// Control workers
            WorkerController(workers, resources, bases, gameInfo, enoughWorkers, resourcesFarmed); 
        }

        // Control light units
        PrioritiseWorkers(light, enemyUnits);
        
        // Control heavy units
        PrioritiseBases(heavy, enemyUnits);
        
        // Control ranged units
        PrioritiseHeavy(ranged, enemyUnits);
        
        return translateActions(playerNumber, gameState);
    }
    
    // Adds all friendly units to their respective lists
    public void AddUnitsToLists(Map <String, List<Unit>> friendlyUnits, int playerNumber, PhysicalGameState physicalGameState) 
    {
    	for (Unit unit : physicalGameState.getUnits()) 
    	{
    		// Get unit type
			UnitType unitType = unit.getType();
			
    		// Check if they are my units
    		if(unit.getPlayer() == playerNumber) 
    		{	
    			// Add base
    			if(unitType == baseType) 
    			{
    				friendlyUnits.get("bases").add(unit);
    			}
    			// Ass barracks
    			else if(unitType == barracksType) 
    			{
    				friendlyUnits.get("barracks").add(unit);
    			}
    			// Add harvesting workers
    			else if(unitType.canHarvest) 
    			{
    				friendlyUnits.get("workers").add(unit);
    			}
    			// Add light units
    			else if(unitType == lightType)
    			{
    				friendlyUnits.get("light").add(unit);
    			}
    			// Add heavy units
    			else if(unitType == heavyType)
    			{
    				friendlyUnits.get("heavy").add(unit);
    			}
    			// Add ranged units
    			else if(unitType == rangedType)
    			{
    				friendlyUnits.get("ranged").add(unit);
    			}
    		}
    		else 
    		{
    			if(unitType == resourceType) 
    			{
    				friendlyUnits.get("resources").add(unit);
    			}
    		}
    	}
    }
    
 // Adds all friendly units to their respective lists
    public void AddEnemyUnitsToLists(Map <String, List<Unit>> friendlyUnits, int playerNumber, PhysicalGameState physicalGameState) 
    {
    	for (Unit unit : physicalGameState.getUnits()) 
    	{
    		// Check if they are enemy units
    		if(unit.getPlayer() != playerNumber) 
    		{	
    			// Get unit type
    			UnitType unitType = unit.getType();
    			
    			// Add base
    			if(unitType == baseType) 
    			{
    				friendlyUnits.get("bases").add(unit);
    			}
    			// Ass barracks
    			else if(unitType == barracksType) 
    			{
    				friendlyUnits.get("barracks").add(unit);
    			}
    			// Add harvesting workers
    			else if(unitType.canHarvest) 
    			{
    				friendlyUnits.get("workers").add(unit);
    			}
    			// Add light units
    			else if(unitType == lightType)
    			{
    				friendlyUnits.get("light").add(unit);
    			}
    			// Add heavy units
    			else if(unitType == heavyType)
    			{
    				friendlyUnits.get("heavy").add(unit);
    			}
    			// Add ranged units
    			else if(unitType == rangedType)
    			{
    				friendlyUnits.get("ranged").add(unit);
    			}
    		}
    	}
    }
    
    // __START OF UNIT CONTROLLERS__
    
    // Control base unit production
    public boolean BaseController(List<Unit> bases,  Map <UnitType, Integer> gameInfo)
    {
    	// Return if no bases in list
    	if(bases.isEmpty()) 
    	{
    		return true;
    	}
    	
    	// Variables for creating workers based on resources
    	int totalFarmableResourcesNumber = gameInfo.get(resourceType);
    	int numberOfWorkers = gameInfo.get(workerType);
    	double targetNumberOfWorkers = Math.ceil(totalFarmableResourcesNumber/2);
    	
    	
    	// Check if there are enough workers
    	if(numberOfWorkers < targetNumberOfWorkers) {
    		// Create a worker
    		train(bases.get(0), workerType);
    		return false;
    	} 
    	else
    	{
    		// Let barracks train workers
    		return true;
    		//System.out.println("Enough workers");
    	}
    }
    
 // Control barracks unit production
    public void BarracksController(List<Unit> barracks,  Map <UnitType, Integer> gameInfo)
    {
    	// Return if no bases in list
    	if(barracks.isEmpty()) {return;}
    	
    	// Variables for creating defensive ranged units
    	int numberOfRanged = gameInfo.get(rangedType);
    	int targetNumberOfRanged = 2;
    	
    	// Check if there are enough ranged units
    	//if(numberOfRanged < targetNumberOfRanged)
    	//{
    		train(barracks.get(0), rangedType);
    	//} 
    	//else 
    	//{
    		// Train light units
    		//train(barracks.get(0), lightType);
    	//}
    	
    	
    	//System.out.println(totalFarmableResourcesNumber);
    }
    
    // Control the workers
    public void WorkerController(List<Unit> workers, List<Unit> resources, List<Unit> bases, Map <UnitType, Integer> gameInfo, boolean enoughWorkers, int resourcesFarmed)
	{
    	
		//List<Unit> freeWorkers = new LinkedList<Unit>();
		//freeWorkers.addAll(workers);
    	
    	// Return if no workers in list
		if(workers.isEmpty()) 
		{
			return;
		}
		
		if(gameInfo.get(barracksType) == 0 && enoughWorkers == true && barracksType.cost < resourcesFarmed)
		{
			if(bases.get(0).getY() < 5)
			{
				build(workers.get(0), barracksType, bases.get(0).getX() + 3, bases.get(0).getY() - 1);
			}
			else
			{
				build(workers.get(0), barracksType, bases.get(0).getX() - 3, bases.get(0).getY() + 1);
			}
		
			// Remove worker who is building
			workers.remove(0);
		}
		
		// Tell each worker what to do
		for (Unit worker : workers)
		{
			// Init closest objects
			Unit closestBase = null;
			Unit closestResource = null;
			
			// Init value to store distance value
			int closestDistance = 0;
			
			// Find closes resource
			for(Unit resource : resources)
			{
					int distanceToResource = Math.abs(resource.getX() - worker.getX()) + Math.abs(resource.getY() - worker.getY());
					if(closestResource == null || distanceToResource < closestDistance)
					{
						closestResource = resource;
						closestDistance = distanceToResource;
					}
			}
			
			// Reset distance value
			closestDistance = 0;
			
			// Find closest base
			for(Unit base : bases)
			{
				int distanceToBase = Math.abs(base.getX() - worker.getX()) + Math.abs(base.getY() - worker.getY());
				if(closestBase == null || distanceToBase < closestDistance)
				{
					closestBase = base;
					closestDistance = distanceToBase;
				}
			}
			
			// Harvest resources
			if(closestResource != null && closestBase != null)
			{
				harvest(worker, closestResource, closestBase);
			}
		}
		
	}
    
    // Attack workers first
    public void PrioritiseWorkers(List<Unit> unitGroup, Map <String, List<Unit>> enemyUnits)
    {
    	// Return if no light units in list
    	if(unitGroup.isEmpty())
    	{
    		return;
    	}
    	
    	// What to attack
    	//Unit target = null;
    	
    	
    	// Go through all light units
    	for(Unit unit : unitGroup)
		{
    		// Priority of which units to attack
    		if(enemyUnits.get("workers").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("workers"));
    		}
    		else if(enemyUnits.get("bases").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("bases"));
    		}
    		else if(enemyUnits.get("light").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("light"));
    		}
    		else if(enemyUnits.get("heavy").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("heavy"));
    		}
    		else if(enemyUnits.get("ranged").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("ranged"));
    		}
    		else if(enemyUnits.get("barracks").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("barracks"));
    		}
		}
    }
	
	// Attack heavies first
    public void PrioritiseHeavy(List<Unit> unitGroup, Map <String, List<Unit>> enemyUnits)
    {
    	// Return if no light units in list
    	if(unitGroup.isEmpty())
    	{
    		return;
    	}
    	
    	// What to attack
    	//Unit target = null;
    	
    	
    	// Go through all light units
    	for(Unit unit : unitGroup)
		{
    		// Priority of which units to attack
    		if(enemyUnits.get("heavy").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("heavy"));
    		}
			else if(enemyUnits.get("workers").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("workers"));
    		}
    		else if(enemyUnits.get("bases").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("bases"));
    		}
    		else if(enemyUnits.get("light").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("light"));
    		}
    		else if(enemyUnits.get("ranged").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("ranged"));
    		}
    		else if(enemyUnits.get("barracks").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("barracks"));
    		}
		}
    }
    
    // Attacks bases first
    public void PrioritiseBases(List<Unit> unitGroup, Map <String, List<Unit>> enemyUnits)
    {
    	// Return if no light units in list
    	if(unitGroup.isEmpty())
    	{
    		return;
    	}
    	
    	// What to attack
    	//Unit target = null;
    	
    	
    	// Go through all light units
    	for(Unit unit : unitGroup)
		{
    		// Priority of which units to attack
    		if(enemyUnits.get("bases").size() != 0)
    		{
    			attack(unit, enemyUnits.get("bases").get(0));
    		}
    		else if(enemyUnits.get("workers").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("workers"));
    		} 
    		else if(enemyUnits.get("barracks").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("barracks"));
    		}
    		else if(enemyUnits.get("light").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("light"));
    		}
    		else if(enemyUnits.get("heavy").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("heavy"));
    		}
    		else if(enemyUnits.get("ranged").size() != 0)
    		{
    			AttackNearest(unit, enemyUnits.get("ranged"));
    		}
		}
    }
    
    // Control ranged units
    public void AttackNearest(Unit playerUnit, List<Unit> enemyUnits)
    {
    	Unit closestEnemy = null;
    	int closestDistance = 0;
    	
    	for(Unit enemy : enemyUnits)
		{
    		int distanceToWorker = Math.abs(enemy.getX() - playerUnit.getX()) + Math.abs(enemy.getY() - playerUnit.getY());
    		
			if(closestEnemy == null || distanceToWorker < closestDistance)
			{
				closestEnemy = enemy;
				closestDistance = distanceToWorker;
			}
		}
    	
    	attack(playerUnit, closestEnemy);
    }
    // __END OF UNIT CONTROLLERS__
    
    public List<ParameterSpecification> getParameters()
    {
        return new ArrayList<>();
    }
    
}
