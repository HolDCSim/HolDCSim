package stat;

import java.util.*;

/**
 * @author fan
 * collection for classes that dumps historical data need to register on  
 */
/**
 * @author fan
 * 
 */
public class HisCollection {

	private HashMap<String, HisCollectable> hisCollection;

	public HisCollection() {
		hisCollection = new HashMap<String, HisCollectable>();
	}

	/**
	 * @param name of the history info
	 * @param source of the history info register the history source to history
	 *            collection
	 */
	public void registerCollection(String name, HisCollectable source) {
		hisCollection.put(name, source);
	}

	public void dumpAllHistories() {
		for (Map.Entry<String, HisCollectable> hisEntry : hisCollection
				.entrySet()) {
			HisCollectable collectable = hisEntry.getValue();
			collectable.dumpHistory();
		}
	}

}
