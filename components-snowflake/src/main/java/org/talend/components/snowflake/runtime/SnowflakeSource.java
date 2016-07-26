package org.talend.components.snowflake.runtime;

import java.util.ArrayList;
import java.util.List;

import org.talend.components.api.component.runtime.BoundedReader;
import org.talend.components.api.component.runtime.BoundedSource;
import org.talend.components.api.container.RuntimeContainer;
import org.talend.components.snowflake.tsnowflakeinput.TSnowflakeInputProperties;

/**
 * The SnowflakeSource provides the mechanism to supply data to other
 * components at run-time.
 *
 * Based on the Apache Beam project, the Source mechanism is appropriate to
 * describe distributed and non-distributed data sources and can be adapted
 * to scalable big data execution engines on a cluster, or run locally.
 *
 * This example component describes an input source that is guaranteed to be
 * run in a single JVM (whether on a cluster or locally), so:
 *
 * <ul>
 * <li>the simplified logic for reading is found in the {@link SnowflakeReader},
 *     and</li>
 * </ul>
 */
public class SnowflakeSource extends SnowflakeSourceOrSink implements BoundedSource {

    public SnowflakeSource() {
    }
    
	
	@Override
    public List<? extends BoundedSource> splitIntoBundles(long desiredBundleSizeBytes, RuntimeContainer adaptor)
            throws Exception {
        List<BoundedSource> list = new ArrayList<>();
        list.add(this);
        return list;
    }

    @Override
    public long getEstimatedSizeBytes(RuntimeContainer adaptor) {
        return 0;
    }

    @Override
    public boolean producesSortedKeys(RuntimeContainer adaptor) {
        return false;
    }
    
    @Override
    public BoundedReader createReader(RuntimeContainer container) {
    	if (properties instanceof TSnowflakeInputProperties) {
    		TSnowflakeInputProperties sfInProps = (TSnowflakeInputProperties) properties;
    		//return new SnowflakeInputReader(adaptor, this, sfInProperties); //TODO: Create and Snowflake Input Reader and return it.
    	} //else if ... check other types and instantiate appropritate Readers
    	
    	return null;
    }



}
