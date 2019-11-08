package utility;

import java.util.Vector;

public class FakeRecordVector<E> extends Vector<E> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private E lastRecord = null;
	private boolean inserted = false;

	@Override
	public boolean add(E e) {
		lastRecord = e;
		if (!inserted) {
			/*
			 * this will cause exception when FakeRecordVector is called out of
			 * "scope"
			 */
			// super.add(e);
			inserted = true;
		}
		return true;
	}

	@Override
	public E lastElement() {
		return lastRecord;
	}

	@Override
	public int size() {
		if (lastRecord == null)
			return 0;
		else
			return 1;

	}

}
