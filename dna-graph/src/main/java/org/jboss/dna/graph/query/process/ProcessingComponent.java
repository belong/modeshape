/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.process;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.DynamicOperand;
import org.jboss.dna.graph.query.model.FullTextSearchScore;
import org.jboss.dna.graph.query.model.Length;
import org.jboss.dna.graph.query.model.LowerCase;
import org.jboss.dna.graph.query.model.NodeLocalName;
import org.jboss.dna.graph.query.model.NodeName;
import org.jboss.dna.graph.query.model.PropertyValue;
import org.jboss.dna.graph.query.model.UpperCase;
import org.jboss.dna.graph.query.validate.Schemata.Column;
import org.jboss.dna.graph.query.validate.Schemata.Table;

/**
 * A component that performs (some) portion of the query processing by {@link #execute() returning the tuples} that result from
 * this stage of processing. Processing components are designed to be assembled into a processing structure, with a single
 * component at the top that returns the results of a query.
 */
@NotThreadSafe
public abstract class ProcessingComponent {

    private final QueryContext context;
    private final Columns columns;

    protected ProcessingComponent( QueryContext context,
                                   Columns columns ) {
        this.context = context;
        this.columns = columns;
        assert this.context != null;
        assert this.columns != null;
    }

    /**
     * Get the context in which this query is being executed.
     * 
     * @return context
     */
    public final QueryContext getContext() {
        return context;
    }

    /**
     * Get the column definitions.
     * 
     * @return the column mappings; never null
     */
    public final Columns getColumns() {
        return columns;
    }

    /**
     * Get the container for problems encountered during processing.
     * 
     * @return the problems container; never null
     */
    protected final Problems problems() {
        return context.getProblems();
    }

    /**
     * Execute this stage of processing and return the resulting tuples that each conform to the {@link #getColumns() columns}.
     * 
     * @return the list of tuples, where each tuple corresonds to the {@link #getColumns() columns}; never null
     */
    public abstract List<Object[]> execute();

    /**
     * Close these results, allowing any resources to be released.
     */
    public void close() {
    }

    /**
     * Utility method to create a new tuples list that is empty.
     * 
     * @return the empty tuples list; never null
     */
    protected List<Object[]> emptyTuples() {
        return new ArrayList<Object[]>(0);
    }

    /**
     * Interface for evaluating a {@link DynamicOperand} to return the resulting value.
     */
    protected static interface DynamicOperation {
        /**
         * Get the expected type of the result of this evaluation
         * 
         * @return the property type; never null
         */
        PropertyType getExpectedType();

        /**
         * Perform the dynamic evaluation to obtain the desired result.
         * 
         * @param tuple the tuple; never null
         * @return the value that results from dynamically evaluating the operand against the tuple; may be null
         */
        Object evaluate( Object[] tuple );
    }

    /**
     * Create a {@link DynamicOperation} instance that is able to evaluate the supplied {@link DynamicOperand}.
     * 
     * @param context the context in which the query is being evaluated; may not be null
     * @param columns the definition of the result columns and the tuples; may not be null
     * @param operand the dynamic operand that is to be evaluated by the returned object; may not be null
     * @return the dynamic operand operation; never null
     */
    protected DynamicOperation createDynamicOperation( QueryContext context,
                                                       Columns columns,
                                                       DynamicOperand operand ) {
        assert operand != null;
        assert columns != null;
        assert context != null;
        final ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            Name propertyName = propValue.getPropertyName();
            String selectorName = propValue.getSelectorName().getName();
            final int index = columns.getColumnIndexForProperty(selectorName, propertyName);
            // Find the expected property type of the value ...
            Table table = context.getSchemata().getTable(propValue.getSelectorName());
            Column schemaColumn = table.getColumn(stringFactory.create(propertyName));
            final PropertyType expectedType = schemaColumn.getPropertyType();
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return expectedType;
                }

                public Object evaluate( Object[] tuple ) {
                    return tuple[index];
                }
            };
        }
        if (operand instanceof Length) {
            Length length = (Length)operand;
            PropertyValue value = length.getPropertyValue();
            Name propertyName = value.getPropertyName();
            String selectorName = value.getSelectorName().getName();
            final int index = columns.getColumnIndexForProperty(selectorName, propertyName);
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.LONG; // length is always LONG
                }

                public Object evaluate( Object[] tuple ) {
                    Object value = tuple[index];
                    // Determine the length ...
                    if (value instanceof Binary) {
                        return ((Binary)value).getStream();
                    }
                    String result = stringFactory.create(value);
                    return result != null ? result.length() : null;
                }
            };
        }
        if (operand instanceof LowerCase) {
            LowerCase lowerCase = (LowerCase)operand;
            final DynamicOperation delegate = createDynamicOperation(context, columns, lowerCase.getOperand());
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.STRING;
                }

                public Object evaluate( Object[] tuple ) {
                    String result = stringFactory.create(delegate.evaluate(tuple));
                    return result != null ? result.toLowerCase() : null;
                }
            };
        }
        if (operand instanceof UpperCase) {
            UpperCase upperCase = (UpperCase)operand;
            final DynamicOperation delegate = createDynamicOperation(context, columns, upperCase.getOperand());
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.STRING;
                }

                public Object evaluate( Object[] tuple ) {
                    String result = stringFactory.create(delegate.evaluate(tuple));
                    return result != null ? result.toUpperCase() : null;
                }
            };
        }
        if (operand instanceof NodeName) {
            NodeName nodeName = (NodeName)operand;
            final int locationIndex = columns.getLocationIndex(nodeName.getSelectorName().getName());
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.STRING;
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    Path path = location.getPath();
                    assert path != null;
                    return path.isRoot() ? "" : stringFactory.create(location.getPath().getLastSegment().getName());
                }
            };
        }
        if (operand instanceof NodeLocalName) {
            NodeLocalName nodeName = (NodeLocalName)operand;
            final int locationIndex = columns.getLocationIndex(nodeName.getSelectorName().getName());
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.STRING;
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    Path path = location.getPath();
                    assert path != null;
                    return path.isRoot() ? "" : location.getPath().getLastSegment().getName().getLocalName();
                }
            };
        }
        if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore score = (FullTextSearchScore)operand;
            String selectorName = score.getSelectorName().getName();
            final int index = columns.getFullTextSearchScoreIndexFor(selectorName);
            if (index < 0) {
                // No full-text search score for this selector, so return 0.0d;
                return new DynamicOperation() {
                    public PropertyType getExpectedType() {
                        return PropertyType.DOUBLE;
                    }

                    public Object evaluate( Object[] tuple ) {
                        return new Double(0.0d);
                    }
                };
            }
            return new DynamicOperation() {
                public PropertyType getExpectedType() {
                    return PropertyType.DOUBLE;
                }

                public Object evaluate( Object[] tuple ) {
                    return tuple[index];
                }
            };
        }
        assert false;
        return null;
    }

    protected Comparator<Object[]> createSortComparator( QueryContext context,
                                                         Columns columns ) {
        assert context != null;
        final int numLocations = columns.getLocationCount();
        assert numLocations > 0;
        final Comparator<Location> typeComparator = Location.comparator();
        if (numLocations == 1) {
            // We can do this a tad faster if we know there is only one Location object ...
            final int locationIndex = columns.getColumnCount();
            return new Comparator<Object[]>() {
                public int compare( Object[] tuple1,
                                    Object[] tuple2 ) {
                    Location value1 = (Location)tuple1[locationIndex];
                    Location value2 = (Location)tuple2[locationIndex];
                    return typeComparator.compare(value1, value2);
                }
            };
        }
        final int firstLocationIndex = columns.getColumnCount();
        return new Comparator<Object[]>() {
            public int compare( Object[] tuple1,
                                Object[] tuple2 ) {
                int result = 0;
                for (int locationIndex = firstLocationIndex; locationIndex != numLocations; ++locationIndex) {
                    Location value1 = (Location)tuple1[locationIndex];
                    Location value2 = (Location)tuple2[locationIndex];
                    result = typeComparator.compare(value1, value2);
                    if (result != 0) return result;
                }
                return result;
            }
        };
    }
}