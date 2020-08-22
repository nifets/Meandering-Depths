package utils

import scala.collection.mutable.HashMap
import scala.collection.mutable.Set

/** Mutable data structure that maps each key to a set of values */

class Multimap[K, V] {
    /** Internal representation*/
    private val table = new HashMap[K, Set[V]]

    /** Add an association to the mapping. */
    def +=(pair: (K,V)) = {
        val (key, value) = pair
        table get key match {
            case Some(set) => table += key -> (set + value)
            case None => table += (key -> Set[V](value))
        }
    }

    /** Remove an asssociation from the mapping. */
    def -=(pair: (K,V)) = {
        val (key, value) = pair
        table get key match {
            case Some(set) => {
                val newSet = set - value
                if (newSet.isEmpty)
                    table -= key
                else
                    table += key -> newSet
            }
            case None =>
        }
    }

    /** Find the set for a specified key, or return an empty set if there is no set associated with that key. */
    def apply(key: K): Set[V] = table.get(key) match {
        case Some(set) => set
        case None => Set[V]()
    }
}
