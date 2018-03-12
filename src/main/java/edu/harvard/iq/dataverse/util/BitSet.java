package edu.harvard.iq.dataverse.util;

import java.io.Serializable;
import java.util.EnumSet;

/**
 * A set, backed by a single {@code long}.
 * @author michael
 */
public class BitSet implements Serializable {

	private long store = 0l;
	
	/**
     * Creates a new set with all bits set to 0.
     * @return a new, empty, set.
     */
    public static BitSet emptySet() {
		return new BitSet();
	}
	
    /**
     * Creates a new set with all bits set to 1.
     * @return a new, full, set.
     */
	public static BitSet fullSet() {
		return new BitSet( ~0 );
	}
	
	/**
	 * Returns all the indices a bit set may have. Can be used for 
	 * cheap for-each loops (i.e. no boxing/unboxing).
	 * @return All the indices a BitSet has [0..63]
	 */
	public static short[] allIndices() {
		short[] retVal = new short[64];
		for ( short s=0; s<64; s++ ) retVal[s]=s;
		return retVal;
	}
	
	public static BitSet from( EnumSet<?> es ) {
		if ( es.isEmpty() ) return emptySet();
		BitSet retVal = new BitSet();
		for ( Enum e : es ) {
			retVal.set( e.ordinal() );
		}
		return retVal;
	}

	public BitSet() {}
	
	public BitSet( BitSet other ) {
		store = other.getBits();
	}
	
	public BitSet( long initial ) {
		store = initial;
	}
	
	public <E extends Enum<E>> EnumSet<E> asSetOf( Class<E> enumClass ) {
		EnumSet<E> retVal = EnumSet.noneOf(enumClass);
		if ( isEmpty() ) return retVal;
		for ( E e : EnumSet.allOf(enumClass) ) {
			if ( isSet(e.ordinal()) ) {
				retVal.add(e);
			}
		}
		return retVal;
	}
	
	public boolean isEmpty() {
		return store == 0;
	}
	
	public BitSet set( int idx, boolean value ) {
		return value ? set(idx) : unset(idx);
	}
	
	public BitSet set( int idx ) {
		store = store | (1l<<idx);
		return this;
	}
	
	public BitSet unset( int idx ) {
		store = store & (~(1l<<idx));
		return this;
	}
	
	public boolean isSet( int idx ) {
		return ( (store&(1l<<idx)) != 0 );
	}
	
	public BitSet copy() {
		return new BitSet( this );
	}
	
	/**
	 * Adds {@code other} to {@code this} set.
	 * @param other The we union with.
	 * @return {@code this}, for call chaining.
	 */
	public BitSet union( BitSet other ) {
		store = store | other.getBits();
		return this;
	}
	
	public BitSet intersect( BitSet other ) {
		store = store & other.getBits();
		return this;
	}
	
	public BitSet xor( BitSet other ) {
		store = store ^ other.getBits();
		return this;
	}
	
	public long getBits() {
		return store;
	}
}
