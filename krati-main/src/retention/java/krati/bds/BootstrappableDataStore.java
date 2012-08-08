package krati.bds;

import krati.bds.marker.AdvanceMarker;

public interface BootstrappableDataStore<T, A extends AdvanceMarker> extends BDSWriter<T, A>, BDSReader<T, A> {

}
