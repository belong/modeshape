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

/**
 * This class provides basic parsing of SQL-92 based DDL files.  The initial implementation does NOT handle generic SQL query
 * statements, but rather database schema manipulation (i.e. CREATE, DROP, ALTER, etc...)
 * 
 */
package org.jboss.dna.sequencer.ddl;

import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.CHECK_SEARCH_CONDITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.COLLATION_NAME;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_ATTRIBUTE_TYPE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_TYPE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.CREATE_VIEW_QUERY_EXPRESSION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_EXPRESSION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_CHAR_INDEX;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_COLUMN_NUMBER;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_OPTION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_PRECISION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DEFAULT_VALUE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.DROP_BEHAVIOR;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.MESSAGE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.NAME;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.NULLABLE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.PROBLEM_LEVEL;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.PROPERTY_VALUE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TEMPORARY;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ADD_TABLE_CONSTRAINT_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_DOMAIN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_REFERENCE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_ASSERTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_CHARACTER_SET_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_COLLATION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_DOMAIN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_SCHEMA_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TRANSLATION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_ASSERTION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_CHARACTER_SET_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLLATION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_DOMAIN_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_SCHEMA_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_CONSTRAINT_DEFINITION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TRANSLATION_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_VIEW_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_FK_COLUMN_REFERENCE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_INSERT_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_MISSING_TERMINATOR;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_SET_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_STATEMENT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_STATEMENT_OPTION;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_TABLE_CONSTRAINT;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_TABLE_REFERENCE;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.VALUE;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.text.Position;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.sequencer.ddl.DdlTokenStream.DdlTokenizer;
import org.jboss.dna.sequencer.ddl.datatype.DataType;
import org.jboss.dna.sequencer.ddl.datatype.DataTypeParser;
import org.jboss.dna.sequencer.ddl.node.AstNode;
import org.jboss.dna.sequencer.ddl.node.AstNodeFactory;

/**
 * Standard SQL 92 DDL file content parser.
 */
public class StandardDdlParser implements DdlParser, DdlConstants, DdlConstants.StatementStartPhrases {
    private final String parserId = "SQL92";

    private boolean testMode = false;

    private List<DdlParserProblem> problems;

    private AstNodeFactory nodeFactory;

    private AstNode rootNode;

    private DdlTokenStream tokens;
    private List<String> allDataTypeStartWords = null;
    private List<Name> validSchemaChildTypes = null;
    private DataTypeParser datatypeParser = null;

    private String terminator = DEFAULT_TERMINATOR;

    private boolean useTerminator = false;

    private Position currentMarkedPosition;

    public StandardDdlParser() {
        super();
        setDoUseTerminator(true);
        setDatatypeParser(new DataTypeParser());
        nodeFactory = new AstNodeFactory();
        problems = new ArrayList<DdlParserProblem>();
        validSchemaChildTypes = new ArrayList<Name>();
    }

    /**
     * Method provide means for DB-specific Statement implementations can contribute DDL Start Phrases and Keywords. These words
     * are critical pieces of data the parser needs to segment the DDL file into statements. These statements all begin with
     * unique start phrases like: CREATE TABLE, DROP VIEW, ALTER TABLE. The base method provided here registers the set of SQL 92
     * based start phrases as well as the set of SQL 92 reserved words (i.e. CREATE, DROP, SCHEMA, CONSTRAINT, etc...).
     * 
     * @param tokens the token stream containing the tokenized DDL content. may not be null
     */
    public void registerWords( DdlTokenStream tokens ) {
        this.tokens = tokens;

        registerKeyWords(SQL_92_RESERVED_WORDS);

        registerStatementStartPhrase(SQL_92_ALL_PHRASES);

        registerSchemaChildTypes(getValidSchemaChildTypes());
    }

    /**
     * Returns the data type parser instance.
     * 
     * @return the {@link DataTypeParser}
     */
    public DataTypeParser getDatatypeParser() {
        return datatypeParser;
    }

    /**
     * @param datatypeParser
     */
    public void setDatatypeParser( DataTypeParser datatypeParser ) {
        this.datatypeParser = datatypeParser;
    }

    /**
     * Method to access the node utility class.
     * 
     * @return the instance of the {@link AstNodeFactory} node utility class
     */
    public AstNodeFactory nodeFactory() {
        return this.nodeFactory;
    }

    /**
     * @return rootNode
     */
    public AstNode getRootNode() {
        return rootNode;
    }

    /**
     * @param rootNode Sets rootNode to the specified value.
     */
    public void setRootNode( AstNode rootNode ) {
        this.rootNode = rootNode;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.DdlParser#getNumberOfKeyWords(org.jboss.dna.sequencer.ddl.DdlTokenStream)
     */
    public int getNumberOfKeyWords( DdlTokenStream tokens ) {
        int count = 0;

        while (tokens.hasNext()) {
            if (tokens.isNextKeyWord()) {
                count++;
            }
            tokens.consume();
        }

        return count;
    }

    /**
     * Parses a DDL string and adds discovered child {@link AstNode}s and properties. This method instantiates the tokenizer,
     * calls a method to allow subclasses to register keywords and statement start phrases with the tokenizer and finally performs
     * the tokenizing (i.e. tokens.start()) before calling the actual parse method.
     * 
     * @param ddl the input string to parse; may not be null
     * @param rootNode the top level {@link AstNode}; may not be null
     * @return true if parsing successful (i.e. no problems were discovered during parsing)
     * @throws ParsingException
     */
    public boolean parse( String ddl,
                          AstNode rootNode ) throws ParsingException {
        assert ddl != null;
        assert rootNode != null;

        tokens = new DdlTokenStream(ddl, DdlTokenStream.ddlTokenizer(false), false);

        registerWords(tokens);

        tokens.start();

        return parse(tokens, rootNode);
    }

    /**
     * Parses DDL content from the {@link DdlTokenStream} provided. Parsed data is converted to an AST via {@link AstNode}s. This
     * tree, represents all recognizable statements and properties within a DDL file. Note that db-specific dialects will need to
     * override many methods, as well as add methods to fully parse their specific DDL.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param rootNode the top level {@link AstNode}; may not be null
     * @return true if parsing successful (i.e. no problems were discovered during parsing)
     * @throws ParsingException
     */
    public boolean parse( DdlTokenStream tokens,
                          AstNode rootNode ) throws ParsingException {
        assert tokens != null;
        assert rootNode != null;

        testPrint("\n== >> StandardDdlParser.parse() PARSING STARTED: ");

        setRootNode(rootNode);

        problems.clear();

        // Check the header of the DDL file (first tokens) to see if they are coded to a specific database dialect
        if (tokens.canConsume("PARSER_ID")) {
            tokens.consume("=");
            String parserType = tokens.consume();

            if (!parserType.equalsIgnoreCase(getId())) {
                String msg = "Incompatable parser ID = " + parserType + " Expected = " + getId();
                throw new DdlParserProblem(DdlConstants.Problems.ERROR, new Position(-1, 1, 0), msg);
            }
        }

        // Simply move to the next statement start (registered prior to tokenizing).

        while (moveToNextStatementStart(tokens)) {

            // It is assumed that if a statement is registered, the registering dialect will handle the parsing of that object
            // and successfully create a statement {@link AstNode}
            AstNode stmtNode = parseNextStatement(tokens, rootNode);

            if (stmtNode == null) {
                markStartOfStatement(tokens);

                String stmtName = tokens.consume();
                stmtNode = parseIgnorableStatement(tokens, stmtName, rootNode);

                markEndOfStatement(tokens, stmtNode);
            }
            // testPrint("== >> Found Statement" + "(" + (++count) + "):\n" + stmtNode);
        }

        rewrite(tokens, rootNode);

        List<DdlParserProblem> copyOfProblems = new ArrayList<DdlParserProblem>(problems);
        for (DdlParserProblem problem : copyOfProblems) {
            attachNewProblem(problem, rootNode);
        }

        // testPrint("== >> StandardDdlParser.parse() PARSING COMPLETE: " + statements.size() + " statements parsed.\n\n");
        int count = 0;
        for (AstNode child : rootNode.getChildren()) {
            testPrint("== >> Found Statement" + "(" + (++count) + "):\n" + child);
        }

        return problems.isEmpty();
    }

    /**
     * Performs token match checks for initial statement type and delegates to specific parser methods. If no specific statement
     * is found, then a call is made to parse a custom statement type. Subclasses may override this method, but the
     * {@link StandardDdlParser}.parseCustomStatement() method is designed to allow for parsing db-specific statement types.
     * 
     * @param tokens the tokenized {@link DdlTokenStream} of the DDL input content; may not be null
     * @param node the top level {@link AstNode}; may not be null
     * @return node the new statement node
     */
    protected AstNode parseNextStatement( DdlTokenStream tokens,
                                          AstNode node ) {
        assert tokens != null;
        assert node != null;

        AstNode stmtNode = null;

        if (tokens.matches(CREATE)) {
            stmtNode = parseCreateStatement(tokens, node);
        } else if (tokens.matches(ALTER)) {
            stmtNode = parseAlterStatement(tokens, node);
        } else if (tokens.matches(DROP)) {
            stmtNode = parseDropStatement(tokens, node);
        } else if (tokens.matches(INSERT)) {
            stmtNode = parseInsertStatement(tokens, node);
        } else if (tokens.matches(SET)) {
            stmtNode = parseSetStatement(tokens, node);
        } else if (tokens.matches(GRANT)) {
            stmtNode = parseGrantStatement(tokens, node);
        }

        if (stmtNode == null) {
            stmtNode = parseCustomStatement(tokens, node);
        }

        return stmtNode;
    }

    private boolean moveToNextStatementStart( DdlTokenStream tokens ) throws ParsingException {
        assert tokens != null;

        StringBuffer sb = new StringBuffer();
        DdlParserProblem problem = null;

        // Check to see if any more tokens exists
        if (tokens.hasNext()) {
            while (tokens.hasNext()) {
                // If the next toke is a STATEMENT_KEY, then stop
                if (!tokens.matches(DdlTokenizer.STATEMENT_KEY)) {
                    // If the next toke is NOT a statement, create a problem statement in case it can't be fully recognized as
                    // a statement.
                    if (problem == null) {
                        markStartOfStatement(tokens);

                        String msg = DdlSequencerI18n.unusedTokensDiscovered.text(tokens.nextPosition().getLine(),
                                                                                  tokens.nextPosition().getColumn());
                        problem = new DdlParserProblem(DdlConstants.Problems.WARNING, tokens.nextPosition(), msg);
                    }

                    String nextTokenValue = null;

                    // For known, parsed statements, the terminator is consumed in the markEndOfStatement() method. So if we get
                    // here, we then we know we've got an unknown statement.
                    if (tokens.matches(getTerminator()) && sb.length() > 0) {
                        nextTokenValue = getTerminator();
                        // Let's call this a statement up until now
                        AstNode unknownNode = unknownTerminatedNode(getRootNode());
                        markEndOfStatement(tokens, unknownNode);
                        // We've determined that it's just an unknown node, which we determine is not a problem node.
                        problem = null;
                    } else {
                        // Just keep consuming, but check each token value and allow sub-classes to handle the token if they wish.
                        // ORACLE, for instance can terminator a complex statement with a backslash, '/'. Calling
                        // handleUnknownToken() allows that dialect to create it's own statement node that can be assessed and
                        // used during the rewrite() call at the end of parsing.
                        nextTokenValue = tokens.consume();
                        AstNode unknownNode = handleUnknownToken(tokens, nextTokenValue);
                        if (unknownNode != null) {
                            markEndOfStatement(tokens, unknownNode);
                            // We've determined that it's just an unknown node, which we determine is not a problem node.
                            problem = null;
                        }
                    }
                    sb.append(SPACE).append(nextTokenValue);

                } else {
                    // If we have a problem, add it.
                    if (problem != null && sb.length() > 0) {
                        problem.setUnusedSource(sb.toString());
                        addProblem(problem);
                    }
                    return true;
                }
            }

            // If we still have a problem, add it.
            if (problem != null && sb.length() > 0) {
                problem.setUnusedSource(sb.toString());
                addProblem(problem);
            }
        }
        return false;
    }

    public final void addProblem( DdlParserProblem problem,
                                  AstNode node ) {
        addProblem(problem);
        attachNewProblem(problem, node);
    }

    public final void addProblem( DdlParserProblem problem ) {
        problems.add(problem);
    }

    public final List<DdlParserProblem> getProblems() {
        return this.problems;
    }

    public final void attachNewProblem( DdlParserProblem problem,
                                        AstNode parentNode ) {
        assert problem != null;
        assert parentNode != null;

        AstNode problemNode = nodeFactory().node("DDL PROBLEM", parentNode, TYPE_PROBLEM);
        problemNode.setProperty(PROBLEM_LEVEL, problem.getLevel());
        problemNode.setProperty(MESSAGE, problem.toString() + "[" + problem.getUnusedSource() + "]");

        testPrint(problem.toString());
    }

    protected void rewrite( DdlTokenStream tokens,
                            AstNode rootNode ) {
        assert tokens != null;
        assert rootNode != null;
        // Walk the tree and remove any missing missing terminator nodes

        removeMissingTerminatorNodes(rootNode);
    }

    protected void removeMissingTerminatorNodes( AstNode parentNode ) {
        assert parentNode != null;
        // Walk the tree and remove any missing missing terminator nodes

        List<AstNode> copyOfNodes = new ArrayList<AstNode>(parentNode.getChildren());

        for (AstNode child : copyOfNodes) {
            if (nodeFactory().hasMixinType(child, TYPE_MISSING_TERMINATOR)) {
                parentNode.removeChild(child);
            } else {
                removeMissingTerminatorNodes(child);
            }
        }
    }

    /**
     * Merges second node into first node by re-setting expression source and length.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param firstNode the node to merge into; may not be null
     * @param secondNode the node to merge into first node; may not be null
     */
    public void mergeNodes( DdlTokenStream tokens,
                            AstNode firstNode,
                            AstNode secondNode ) {
        assert tokens != null;
        assert firstNode != null;
        assert secondNode != null;

        int firstStartIndex = (Integer)firstNode.getProperty(DDL_START_CHAR_INDEX).getFirstValue();
        int secondStartIndex = (Integer)secondNode.getProperty(DDL_START_CHAR_INDEX).getFirstValue();
        int deltaLength = ((String)secondNode.getProperty(DDL_EXPRESSION).getFirstValue()).length();
        Position startPosition = new Position(firstStartIndex, 1, 0);
        Position endPosition = new Position((secondStartIndex + deltaLength), 1, 0);
        String source = tokens.getContentBetween(startPosition, endPosition);
        firstNode.setProperty(DDL_EXPRESSION, source);
    }

    /**
     * Utility method subclasses can override to check unknown tokens and perform additional node manipulation. Example would be
     * in Oracle dialect for CREATE FUNCTION statements that can end with an '/' character because statement can contain multiple
     * statements.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param tokenValue the string value of the unknown token; never null
     * @return the new node
     * @throws ParsingException
     */
    public AstNode handleUnknownToken( DdlTokenStream tokens,
                                       String tokenValue ) throws ParsingException {
        assert tokens != null;
        assert tokenValue != null;

        // DEFAULT IMPLEMENTATION DOES NOTHING
        return null;
    }

    /**
     * Parses DDL CREATE statement based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseCreateStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        AstNode stmtNode = null;

        // DEFAULT DOES NOTHING
        // Subclasses can implement additional parsing
        // System.out.println(" >>> FOUND [CREATE] STATEMENT: TOKEN = " + tokens.consume() + " " + tokens.consume() + " " +
        // tokens.consume());
        // SQL 92 CREATE OPTIONS:
        // CREATE SCHEMA
        // CREATE DOMAIN
        // CREATE [ { GLOBAL | LOCAL } TEMPORARY ] TABLE
        // CREATE VIEW
        // CREATE ASSERTION
        // CREATE CHARACTER SET
        // CREATE COLLATION
        // CREATE TRANSLATION

        if (tokens.matches(STMT_CREATE_SCHEMA)) {
            stmtNode = parseCreateSchemaStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_TABLE) || tokens.matches(STMT_CREATE_GLOBAL_TEMPORARY_TABLE)
                   || tokens.matches(STMT_CREATE_LOCAL_TEMPORARY_TABLE)) {
            stmtNode = parseCreateTableStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_VIEW) || tokens.matches(STMT_CREATE_OR_REPLACE_VIEW)) {
            stmtNode = parseCreateViewStatement(tokens, parentNode);
        } else if (tokens.matches(STMT_CREATE_ASSERTION)) {
            stmtNode = parseStatement(tokens, STMT_CREATE_ASSERTION, parentNode, TYPE_CREATE_ASSERTION_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_CHARACTER_SET)) {
            stmtNode = parseStatement(tokens, STMT_CREATE_CHARACTER_SET, parentNode, TYPE_CREATE_CHARACTER_SET_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_COLLATION)) {
            stmtNode = parseStatement(tokens, STMT_CREATE_COLLATION, parentNode, TYPE_CREATE_COLLATION_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_TRANSLATION)) {
            stmtNode = parseStatement(tokens, STMT_CREATE_TRANSLATION, parentNode, TYPE_CREATE_TRANSLATION_STATEMENT);
        } else if (tokens.matches(STMT_CREATE_DOMAIN)) {
            stmtNode = parseStatement(tokens, STMT_CREATE_DOMAIN, parentNode, TYPE_CREATE_DOMAIN_STATEMENT);
        } else {
            markStartOfStatement(tokens);

            stmtNode = parseIgnorableStatement(tokens, "CREATE UNKNOWN", parentNode);
            Position position = getCurrentMarkedPosition();
            String msg = DdlSequencerI18n.unknownCreateStatement.text(position.getLine(), position.getColumn());
            DdlParserProblem problem = new DdlParserProblem(DdlConstants.Problems.WARNING, position, msg);

            stmtNode.setProperty(TYPE_PROBLEM, problem.toString());

            markEndOfStatement(tokens, stmtNode);
        }

        return stmtNode;
    }

    /**
     * Parses DDL ALTER statement based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed ALTER {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseAlterStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(ALTER, TABLE)) {
            return parseAlterTableStatement(tokens, parentNode);
        } else if (tokens.matches("ALTER", "DOMAIN")) {
            markStartOfStatement(tokens);

            tokens.consume("ALTER", "DOMAIN");
            String domainName = parseName(tokens);

            AstNode alterNode = nodeFactory().node(domainName, parentNode, TYPE_ALTER_DOMAIN_STATEMENT);

            parseUntilTerminator(tokens);

            markEndOfStatement(tokens, alterNode);

            return alterNode;
        }

        return null;
    }

    /**
     * Parses DDL ALTER TABLE {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed ALTER TABLE {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseAlterTableStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        // <alter table statement> ::=
        // ALTER TABLE <table name> <alter table action>
        //
        // <alter table action> ::=
        // <add column definition>
        // | <alter column definition>
        // | <drop column definition>
        // | <add table constraint definition>
        // | <drop table constraint definition>

        tokens.consume("ALTER", "TABLE"); // consumes 'ALTER'
        String tableName = parseName(tokens);

        AstNode alterTableNode = nodeFactory().node(tableName, parentNode, TYPE_ALTER_TABLE_STATEMENT);

        if (tokens.canConsume("ADD")) {
            if (isTableConstraint(tokens)) {
                parseTableConstraint(tokens, alterTableNode, true);
            } else {
                parseSingleTerminatedColumnDefinition(tokens, alterTableNode, true);
            }

        } else if (tokens.canConsume("DROP")) {
            if (tokens.canConsume("CONSTRAINT")) {
                String constraintName = parseName(tokens); // constraint name

                AstNode constraintNode = nodeFactory().node(constraintName, alterTableNode, TYPE_DROP_TABLE_CONSTRAINT_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    constraintNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            } else {
                // ALTER TABLE supplier
                // DROP COLUMN supplier_name;

                // DROP [ COLUMN ] <column name> <drop behavior>
                tokens.canConsume("COLUMN"); // "COLUMN" is optional

                String columnName = parseName(tokens);

                AstNode columnNode = nodeFactory().node(columnName, alterTableNode, TYPE_DROP_COLUMN_DEFINITION);

                if (tokens.canConsume(DropBehavior.CASCADE)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.CASCADE);
                } else if (tokens.canConsume(DropBehavior.RESTRICT)) {
                    columnNode.setProperty(DROP_BEHAVIOR, DropBehavior.RESTRICT);
                }
            }
        } else if (tokens.canConsume("ALTER")) {
            // EXAMPLE: ALTER TABLE table_name [ ALTER column_name SET DEFAULT (0) ]
            //
            // ALTER [ COLUMN ] <column name> {SET <default clause> | DROP DEFAULT}

            tokens.canConsume("COLUMN");
            String alterColumnName = parseName(tokens);

            AstNode columnNode = nodeFactory().node(alterColumnName, alterTableNode, TYPE_ALTER_COLUMN_DEFINITION);

            if (tokens.canConsume("SET")) {
                parseDefaultClause(tokens, columnNode);
            } else if (tokens.canConsume("DROP", "DEFAULT")) {
                columnNode.setProperty(DROP_BEHAVIOR, "DROP DEFAULT");
            }

        } else {
            parseUntilTerminator(tokens); // COULD BE "NESTED TABLE xxxxxxxx" option clause
        }

        markEndOfStatement(tokens, alterTableNode);

        return alterTableNode;
    }

    /**
     * Parses DDL DROP {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed DROP {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseDropStatement( DdlTokenStream tokens,
                                          AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        if (tokens.matches(STMT_DROP_TABLE)) {
            // <drop table statement> ::=
            // DROP TABLE <table name> <drop behavior>
            //
            // <drop behavior> ::= CASCADE | RESTRICT
            return parseSimpleDropStatement(tokens, STMT_DROP_TABLE, parentNode, TYPE_DROP_TABLE_STATEMENT);
        } else if (tokens.matches(STMT_DROP_VIEW)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_VIEW, parentNode, TYPE_DROP_VIEW_STATEMENT);
        } else if (tokens.matches(STMT_DROP_SCHEMA)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_SCHEMA, parentNode, TYPE_DROP_SCHEMA_STATEMENT);
        } else if (tokens.matches(STMT_DROP_DOMAIN)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_DOMAIN, parentNode, TYPE_DROP_DOMAIN_STATEMENT);
        } else if (tokens.matches(STMT_DROP_TRANSLATION)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_TRANSLATION, parentNode, TYPE_DROP_TRANSLATION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_CHARACTER_SET)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_CHARACTER_SET, parentNode, TYPE_DROP_CHARACTER_SET_STATEMENT);
        } else if (tokens.matches(STMT_DROP_ASSERTION)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_ASSERTION, parentNode, TYPE_DROP_ASSERTION_STATEMENT);
        } else if (tokens.matches(STMT_DROP_COLLATION)) {
            return parseSimpleDropStatement(tokens, STMT_DROP_COLLATION, parentNode, TYPE_DROP_COLLATION_STATEMENT);
        }

        return null;
    }

    private AstNode parseSimpleDropStatement( DdlTokenStream tokens,
                                              String[] startPhrase,
                                              AstNode parentNode,
                                              Name stmtType ) throws ParsingException {
        assert tokens != null;
        assert startPhrase != null && startPhrase.length > 0;
        assert parentNode != null;

        markStartOfStatement(tokens);

        String behavior = null;
        tokens.consume(startPhrase);

        List<String> nameList = new ArrayList<String>();
        nameList.add(parseName(tokens));
        while (tokens.matches(COMMA)) {
            tokens.consume(COMMA);
            nameList.add(parseName(tokens));
        }

        if (tokens.canConsume("CASCADE")) {
            behavior = "CASCADE";
        } else if (tokens.canConsume("RESTRICT")) {
            behavior = "RESTRICT";
        }

        AstNode dropNode = nodeFactory().node(nameList.get(0), parentNode, stmtType);

        if (behavior != null) {
            dropNode.setProperty(DROP_BEHAVIOR, behavior);
        }

        markEndOfStatement(tokens, dropNode);

        return dropNode;
    }

    /**
     * Parses DDL INSERT {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseInsertStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // Original implementation does NOT parse Insert statement, but just returns a generic TypedStatement
        if (tokens.matches(STMT_INSERT_INTO)) {
            markStartOfStatement(tokens);

            tokens.consume(STMT_INSERT_INTO);
            String prefix = getStatementTypeName(STMT_INSERT_INTO);
            AstNode node = nodeFactory().node(prefix, parentNode, TYPE_INSERT_STATEMENT);

            parseUntilTerminator(tokens);

            markEndOfStatement(tokens, node);

            return node;
        }

        return null;
    }

    /**
     * Parses DDL SET {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseSetStatement( DdlTokenStream tokens,
                                         AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // Original implementation does NOT parse Insert statement, but just returns a generic TypedStatement
        if (tokens.matches(SET)) {
            markStartOfStatement(tokens);

            tokens.consume(SET);
            AstNode node = nodeFactory().node("SET", parentNode, TYPE_SET_STATEMENT);

            parseUntilTerminator(tokens);

            markEndOfStatement(tokens, node);
            return node;
        }

        return null;
    }

    /**
     * Parses DDL INSERT {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseGrantStatement( DdlTokenStream tokens,
                                           AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // Original implementation does NOT parse Insert statement, but just returns a generic TypedStatement
        markStartOfStatement(tokens);

        tokens.consume(GRANT);
        String name = GRANT;

        tokens.consume(); // First Privilege token

        AstNode node = nodeFactory().node(name, parentNode, TYPE_GRANT_STATEMENT);

        parseUntilTerminator(tokens);

        markEndOfStatement(tokens, node);

        return node;
    }

    /**
     * Catch-all method to parse unknown (not registered or handled by sub-classes) statements.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseCustomStatement( DdlTokenStream tokens,
                                            AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        // DEFAULT DOES NOTHING
        // Subclasses can implement additional parsing

        return null;
    }

    // ===========================================================================================================================
    // PARSING CREATE TABLE
    // ===========================================================================================================================

    /**
     * Parses DDL CREATE TABLE {@link AstNode} based on SQL 92 specifications.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed CREATE TABLE {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseCreateTableStatement( DdlTokenStream tokens,
                                                 AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);

        tokens.consume(CREATE); // CREATE
        String temporaryValue = null;
        if (tokens.canConsume("LOCAL")) {
            tokens.consume("TEMPORARY");
            temporaryValue = "LOCAL";
        } else if (tokens.canConsume("GLOBAL")) {
            tokens.consume("TEMPORARY");
            temporaryValue = "GLOBAL";
        }

        tokens.consume(TABLE);

        String tableName = parseName(tokens);

        AstNode tableNode = nodeFactory().node(tableName, parentNode, TYPE_CREATE_TABLE_STATEMENT);

        if (temporaryValue != null) {
            tableNode.setProperty(TEMPORARY, temporaryValue);
        }

        // System.out.println("  >> PARSING CREATE TABLE >>  Name = " + tableName);
        parseColumnsAndConstraints(tokens, tableNode);

        parseCreateTableOptions(tokens, tableNode);

        markEndOfStatement(tokens, tableNode);

        return tableNode;
    }

    protected void parseCreateTableOptions( DdlTokenStream tokens,
                                            AstNode tableNode ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        // [ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
        while (areNextTokensCreateTableOptions(tokens)) {
            parseNextCreateTableOption(tokens, tableNode);
        }

    }

    protected void parseNextCreateTableOption( DdlTokenStream tokens,
                                               AstNode tableNode ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        if (tokens.canConsume("ON", "COMMIT")) {
            String option = "";
            // PRESERVE ROWS | DELETE ROWS | DROP
            if (tokens.canConsume("PRESERVE", "ROWS")) {
                option = option + "ON COMMIT PRESERVE ROWS";
            } else if (tokens.canConsume("DELETE", "ROWS")) {
                option = option + "ON COMMIT DELETE ROWS";
            } else if (tokens.canConsume("DROP")) {
                option = option + "ON COMMIT DROP";
            }

            if (option.length() > 0) {
                AstNode tableOption = nodeFactory().node("option", tableNode, TYPE_STATEMENT_OPTION);
                tableOption.setProperty(VALUE, option);
            }
        }
    }

    protected boolean areNextTokensCreateTableOptions( DdlTokenStream tokens ) throws ParsingException {
        assert tokens != null;

        boolean result = false;

        // [ ON COMMIT { PRESERVE ROWS | DELETE ROWS | DROP } ]
        if (tokens.matches("ON", "COMMIT")) {
            result = true;
        }

        return result;
    }

    /**
     * Utility method to parse columns and table constraints within either a CREATE TABLE statement. Method first parses and
     * copies the text enclosed within the bracketed "( xxxx  )" statement. Then the individual column definition or table
     * constraint definition sub-statements are parsed assuming they are comma delimited.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param tableNode
     * @throws ParsingException
     */
    protected void parseColumnsAndConstraints( DdlTokenStream tokens,
                                               AstNode tableNode ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        if (!tokens.matches(L_PAREN)) {
            return;
        }

        String tableElementString = getTableElementsString(tokens, false);

        DdlTokenStream localTokens = new DdlTokenStream(tableElementString, DdlTokenStream.ddlTokenizer(false), false);

        localTokens.start();

        StringBuffer unusedTokensSB = new StringBuffer();
        do {
            if (isTableConstraint(localTokens)) {
                parseTableConstraint(localTokens, tableNode, false);
            } else if (isColumnDefinitionStart(localTokens)) {
                parseColumnDefinition(localTokens, tableNode, false);
            } else {
                unusedTokensSB.append(SPACE).append(localTokens.consume());
            }
        } while (localTokens.canConsume(COMMA));

        if (unusedTokensSB.length() > 0) {
            String msg = DdlSequencerI18n.unusedTokensParsingColumnsAndConstraints.text(tableNode.getProperty(NAME));
            DdlParserProblem problem = new DdlParserProblem(DdlConstants.Problems.WARNING, Position.EMPTY_CONTENT_POSITION, msg);
            problem.setUnusedSource(unusedTokensSB.toString());
            addProblem(problem, tableNode);
        }

    }

    /**
     * Utility method to parse the actual column definition. SQL-92 Structural Specification <column definition> ::= <column name>
     * { <data type> | <domain name> } [ <default clause> ] [ <column constraint definition>... ] [ <collate clause> ]
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param tableNode
     * @param isAlterTable true if in-line constraint is part of add column in alter table statement
     * @throws ParsingException
     */
    protected void parseColumnDefinition( DdlTokenStream tokens,
                                          AstNode tableNode,
                                          boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        tokens.canConsume("COLUMN");
        String columnName = parseName(tokens);
        DataType datatype = getDatatypeParser().parse(tokens);

        AstNode columnNode = nodeFactory().node(columnName, tableNode, TYPE_COLUMN_DEFINITION);

        getDatatypeParser().setPropertiesOnNode(columnNode, datatype);

        // Now clauses and constraints can be defined in any order, so we need to keep parsing until we get to a comma
        StringBuffer unusedTokensSB = new StringBuffer();

        while (tokens.hasNext() && !tokens.matches(COMMA)) {
            boolean parsedDefaultClause = parseDefaultClause(tokens, columnNode);
            if (!parsedDefaultClause) {
                boolean parsedCollate = parseCollateClause(tokens, columnNode);
                boolean parsedConstraint = parseColumnConstraint(tokens, columnNode, isAlterTable);
                if (!parsedCollate && !parsedConstraint) {
                    // THIS IS AN ERROR. NOTHING FOUND.
                    // NEED TO absorb tokens
                    unusedTokensSB.append(SPACE).append(tokens.consume());
                }
            }
            tokens.canConsume(DdlTokenizer.COMMENT);
        }

        if (unusedTokensSB.length() > 0) {
            String msg = DdlSequencerI18n.unusedTokensParsingColumnDefinition.text(tableNode.getName());
            DdlParserProblem problem = new DdlParserProblem(Problems.WARNING, Position.EMPTY_CONTENT_POSITION, msg);
            problem.setUnusedSource(unusedTokensSB.toString());
            addProblem(problem, tableNode);
        }
    }

    /**
     * Utility method to parse the actual column definition. SQL-92 Structural Specification <column definition> ::= <column name>
     * { <data type> | <domain name> } [ <default clause> ] [ <column constraint definition>... ] [ <collate clause> ]
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param tableNode the alter or create table statement node; may not be null
     * @param isAlterTable true if in-line constraint is part of add column in alter table statement
     * @throws ParsingException
     */
    protected void parseSingleTerminatedColumnDefinition( DdlTokenStream tokens,
                                                          AstNode tableNode,
                                                          boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        tokens.canConsume("COLUMN");
        String columnName = parseName(tokens);
        DataType datatype = getDatatypeParser().parse(tokens);

        AstNode columnNode = nodeFactory().node(columnName, tableNode, TYPE_COLUMN_DEFINITION);

        getDatatypeParser().setPropertiesOnNode(columnNode, datatype);
        // Now clauses and constraints can be defined in any order, so we need to keep parsing until we get to a comma, a
        // terminator
        // or a new statement

        while (tokens.hasNext() && !tokens.matches(getTerminator()) && !tokens.matches(DdlTokenizer.STATEMENT_KEY)) {
            boolean parsedDefaultClause = parseDefaultClause(tokens, columnNode);
            if (!parsedDefaultClause) {
                parseCollateClause(tokens, columnNode);
                parseColumnConstraint(tokens, columnNode, isAlterTable);
            }
            consumeComment(tokens);
        }
    }

    /**
     * Method which extracts the table element string from a CREATE TABLE statement.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param useTerminator
     * @return the parsed table elements String.
     * @throws ParsingException
     */
    protected String getTableElementsString( DdlTokenStream tokens,
                                             boolean useTerminator ) throws ParsingException {
        assert tokens != null;

        StringBuffer sb = new StringBuffer(100);

        if (useTerminator) {
            while (!isTerminator(tokens)) {
                sb.append(SPACE).append(tokens.consume());
            }
        } else {
            // Assume we start with open parenthesis '(', then we can count on walking through ALL tokens until we find the close
            // parenthesis ')'. If there are intermediate parenthesis, we can count on them being pairs.
            tokens.consume(L_PAREN); // EXPECTED

            int iParen = 0;
            while (tokens.hasNext()) {
                if (tokens.matches(L_PAREN)) {
                    iParen++;
                } else if (tokens.matches(R_PAREN)) {
                    if (iParen == 0) {
                        tokens.consume(R_PAREN);
                        break;
                    }
                    iParen--;
                }
                if (isComment(tokens)) {
                    tokens.consume();
                } else {
                    sb.append(SPACE).append(tokens.consume());
                }
            }
        }

        return sb.toString();

    }

    /**
     * Simple method which parses, consumes and returns a string representing text found between parenthesis (i.e. '()') If
     * parents don't exist, method returns NULL;
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param includeParens
     * @return the parenthesis bounded text or null if no parens.
     * @throws ParsingException
     */
    protected String consumeParenBoundedTokens( DdlTokenStream tokens,
                                                boolean includeParens ) throws ParsingException {
        assert tokens != null;

        // Assume we start with open parenthesis '(', then we can count on walking through ALL tokens until we find the close
        // parenthesis ')'. If there are intermediate parenthesis, we can count on them being pairs.
        if (tokens.canConsume(L_PAREN)) { // EXPECTED
            StringBuffer sb = new StringBuffer(100);
            if (includeParens) {
                sb.append(L_PAREN);
            }
            int iParen = 0;
            while (tokens.hasNext()) {
                if (tokens.matches(L_PAREN)) {
                    iParen++;
                } else if (tokens.matches(R_PAREN)) {
                    if (iParen == 0) {
                        tokens.consume(R_PAREN);
                        if (includeParens) {
                            sb.append(SPACE).append(R_PAREN);
                        }
                        break;
                    }
                    iParen--;
                }
                if (isComment(tokens)) {
                    tokens.consume();
                } else {
                    sb.append(SPACE).append(tokens.consume());
                }
            }
            return sb.toString();
        }

        return null;
    }

    /**
     * Parses an in-line column constraint including NULLABLE value, UNIQUE, PRIMARY KEY and REFERENCES to a Foreign Key. The
     * values for the constraint are set as properties on the input columnNode.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param columnNode the column definition being created; may not be null
     * @param isAlterTable true if in-line constraint is part of add column in alter table statement
     * @return true if parsed a constraint, else false.
     * @throws ParsingException
     */
    protected boolean parseColumnConstraint( DdlTokenStream tokens,
                                             AstNode columnNode,
                                             boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert columnNode != null;

        Name mixinType = isAlterTable ? TYPE_ADD_TABLE_CONSTRAINT_DEFINITION : TYPE_TABLE_CONSTRAINT;

        boolean result = false;

        // : [ CONSTRAINT <constraint name> ] <column constraint> [ <constraint attributes> ]
        // <column constraint> ::= NOT NULL | <unique specification> | <references specification> | <check constraint definition>
        // <unique specification> ::= UNIQUE | PRIMARY KEY
        // <references specification> ::= REFERENCES <referenced table and columns> [ MATCH <match type> ] [ <referential
        // triggered action> ]
        // <check constraint definition> ::= CHECK <left paren> <search condition> <right paren>
        String colName = columnNode.getName().getString();

        if (tokens.canConsume("NULL")) {
            columnNode.setProperty(NULLABLE, "NULL");
            result = true;
        } else if (tokens.canConsume("NOT", "NULL")) {
            columnNode.setProperty(NULLABLE, "NOT NULL");
            result = true;
        } else if (tokens.matches("CONSTRAINT")) {
            result = true;
            tokens.consume("CONSTRAINT");
            String constraintName = parseName(tokens);
            AstNode constraintNode = nodeFactory().node(constraintName, columnNode.getParent(), mixinType);

            if (tokens.matches("UNIQUE")) {
                // CONSTRAINT P_KEY_2a UNIQUE (PERMISSIONUID)
                tokens.consume("UNIQUE"); // UNIQUE

                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_UC);

                // CONSUME COLUMNS
                parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

                parseConstraintAttributes(tokens, constraintNode);
            } else if (tokens.matches("PRIMARY", "KEY")) {
                // CONSTRAINT U_KEY_2a PRIMARY KEY (PERMISSIONUID)
                tokens.consume("PRIMARY"); // PRIMARY
                tokens.consume("KEY"); // KEY

                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_PK);

                // CONSUME COLUMNS
                parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

                parseConstraintAttributes(tokens, constraintNode);
            } else if (tokens.matches("REFERENCES")) {
                // References in an in-line constraint is really a foreign key definition
                // EXAMPLE:
                // COLUMN_NAME DATATYPE NOT NULL DEFAULT (0) CONSTRAINT SOME_FK_NAME REFERENCES SOME_TABLE_NAME (SOME_COLUMN_NAME,
                // ...)

                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_FK);

                nodeFactory().node(colName, constraintNode, TYPE_COLUMN_REFERENCE);

                parseReferences(tokens, constraintNode);

                parseConstraintAttributes(tokens, constraintNode);
            }
        } else if (tokens.matches("UNIQUE")) {
            result = true;
            tokens.consume("UNIQUE");
            // Unique constraint for this particular column
            String uc_name = "UC_1"; // UNIQUE CONSTRAINT NAME

            AstNode constraintNode = nodeFactory().node(uc_name, columnNode.getParent(), mixinType);

            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_UC);

            nodeFactory().node(colName, constraintNode, TYPE_COLUMN_REFERENCE);

        } else if (tokens.matches("PRIMARY", "KEY")) {
            result = true;
            tokens.consume("PRIMARY", "KEY");
            // PRIMARY KEY for this particular column
            String pk_name = "PK_1"; // PRIMARY KEY NAME

            AstNode constraintNode = nodeFactory().node(pk_name, columnNode.getParent(), mixinType);

            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_PK);

            nodeFactory().node(colName, constraintNode, TYPE_COLUMN_REFERENCE);

        } else if (tokens.matches("FOREIGN", "KEY")) {
            result = true;
            tokens.consume("FOREIGN", "KEY");
            // This is an auto-named FK
            // References in an in-line constraint is really a foreign key definition
            // EXAMPLE:
            // COLUMN_NAME DATATYPE NOT NULL DEFAULT (0) FOREIGN KEY MY_FK_NAME REFERENCES SOME_TABLE_NAME (SOME_COLUMN_NAME, ...)

            String constraintName = parseName(tokens);

            AstNode constraintNode = nodeFactory().node(constraintName, columnNode.getParent(), mixinType);

            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_FK);

            nodeFactory().node(colName, constraintNode, TYPE_COLUMN_REFERENCE);

            parseReferences(tokens, constraintNode);
            parseConstraintAttributes(tokens, constraintNode);
        } else if (tokens.matches("REFERENCES")) {
            result = true;
            // This is an auto-named FK
            // References in an in-line constraint is really a foreign key definition
            // EXAMPLE:
            // COLUMN_NAME DATATYPE NOT NULL DEFAULT (0) REFERENCES SOME_TABLE_NAME (SOME_COLUMN_NAME, ...)

            String constraintName = "FK_1";

            AstNode constraintNode = nodeFactory().node(constraintName, columnNode.getParent(), mixinType);

            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_FK);

            nodeFactory().node(colName, constraintNode, TYPE_COLUMN_REFERENCE);

            parseReferences(tokens, constraintNode);
            parseConstraintAttributes(tokens, constraintNode);
        } else if (tokens.matches("CHECK")) {
            result = true;
            tokens.consume("CHECK"); // CHECK

            String ck_name = "CHECK_1";

            AstNode constraintNode = nodeFactory().node(ck_name, columnNode.getParent(), mixinType);
            constraintNode.setProperty(NAME, ck_name);
            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_C);

            String clause = consumeParenBoundedTokens(tokens, true);
            constraintNode.setProperty(CHECK_SEARCH_CONDITION, clause);
        }

        return result;
    }

    /**
     * Parses full table constraint definition including the "CONSTRAINT" token Examples: CONSTRAINT P_KEY_2a UNIQUE
     * (PERMISSIONUID)
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param tableNode
     * @param isAlterTable true if in-line constraint is part of add column in alter table statement
     * @throws ParsingException
     */
    protected void parseTableConstraint( DdlTokenStream tokens,
                                         AstNode tableNode,
                                         boolean isAlterTable ) throws ParsingException {
        assert tokens != null;
        assert tableNode != null;

        Name mixinType = isAlterTable ? TYPE_ADD_TABLE_CONSTRAINT_DEFINITION : TYPE_TABLE_CONSTRAINT;

        /*
        <table constraint definition> ::=
            [ <constraint name definition> ]
            <table constraint> [ <constraint attributes> ]
        
        <table constraint> ::=
              <unique constraint definition>
            | <referential constraint definition>
            | <check constraint definition>
            
        <constraint attributes> ::=
              <constraint check time> [ [ NOT ] DEFERRABLE ]
            | [ NOT ] DEFERRABLE [ <constraint check time> ]
        
        <unique constraint definition> ::=
                    <unique specification> even in SQL3)
            <unique specification>
              <left paren> <unique column list> <right paren>
        
        <unique column list> ::= <column name list>
        
        <referential constraint definition> ::=
            FOREIGN KEY
                <left paren> <referencing columns> <right paren>
              <references specification>
        
        <referencing columns> ::=
            <reference column list>
            
        <constraint attributes> ::=
              <constraint check time> [ [ NOT ] DEFERRABLE ]
            | [ NOT ] DEFERRABLE [ <constraint check time> ]
        
        <constraint check time> ::=
              INITIALLY DEFERRED
            | INITIALLY IMMEDIATE
            
        <check constraint definition> ::=
        	CHECK
        		<left paren> <search condition> <right paren>
         */
        consumeComment(tokens);

        if ((tokens.matches("PRIMARY", "KEY")) || (tokens.matches("FOREIGN", "KEY")) || (tokens.matches("UNIQUE"))) {

            // This is the case where the PK/FK/UK is NOT NAMED
            if (tokens.matches("UNIQUE")) {
                String uc_name = "UC_1"; // UNIQUE CONSTRAINT NAME
                tokens.consume(); // UNIQUE

                AstNode constraintNode = nodeFactory().node(uc_name, tableNode, mixinType);
                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_UC);

                // CONSUME COLUMNS
                parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

                parseConstraintAttributes(tokens, constraintNode);

                consumeComment(tokens);
            } else if (tokens.matches("PRIMARY", "KEY")) {
                String pk_name = "PK_1"; // PRIMARY KEY NAME
                tokens.consume("PRIMARY", "KEY"); // PRIMARY KEY

                AstNode constraintNode = nodeFactory().node(pk_name, tableNode, mixinType);
                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_PK);

                // CONSUME COLUMNS
                parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

                parseConstraintAttributes(tokens, constraintNode);

                consumeComment(tokens);
            } else if (tokens.matches("FOREIGN", "KEY")) {
                String fk_name = "FK_1"; // FOREIGN KEY NAME
                tokens.consume("FOREIGN", "KEY"); // FOREIGN KEY

                if (!tokens.matches(L_PAREN)) {
                    // Assume the FK is Named here
                    fk_name = tokens.consume();
                }

                AstNode constraintNode = nodeFactory().node(fk_name, tableNode, mixinType);
                constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_FK);

                // CONSUME COLUMNS
                parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

                // Parse the references to table and columns
                parseReferences(tokens, constraintNode);

                parseConstraintAttributes(tokens, constraintNode);

                consumeComment(tokens);
            }
        } else if (tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "UNIQUE")) {
            // CONSTRAINT P_KEY_2a UNIQUE (PERMISSIONUID)
            tokens.consume(); // CONSTRAINT
            String uc_name = parseName(tokens); // UNIQUE CONSTRAINT NAME
            tokens.consume("UNIQUE"); // UNIQUE

            AstNode constraintNode = nodeFactory().node(uc_name, tableNode, mixinType);
            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_UC);

            // CONSUME COLUMNS
            parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

            parseConstraintAttributes(tokens, constraintNode);

            consumeComment(tokens);
        } else if (tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "PRIMARY", "KEY")) {
            // CONSTRAINT U_KEY_2a PRIMARY KEY (PERMISSIONUID)
            tokens.consume("CONSTRAINT"); // CONSTRAINT
            String pk_name = parseName(tokens); // PRIMARY KEY NAME
            tokens.consume("PRIMARY", "KEY"); // PRIMARY KEY

            AstNode constraintNode = nodeFactory().node(pk_name, tableNode, mixinType);
            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_PK);

            // CONSUME COLUMNS
            parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

            parseConstraintAttributes(tokens, constraintNode);

            consumeComment(tokens);

        } else if (tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "FOREIGN", "KEY")) {
            // CONSTRAINT F_KEY_2a FOREIGN KEY (PERMISSIONUID)
            tokens.consume("CONSTRAINT"); // CONSTRAINT
            String fk_name = parseName(tokens); // FOREIGN KEY NAME
            tokens.consume("FOREIGN", "KEY"); // FOREIGN KEY

            AstNode constraintNode = nodeFactory().node(fk_name, tableNode, mixinType);

            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_FK);

            // CONSUME COLUMNS
            parseColumnNameList(tokens, constraintNode, TYPE_COLUMN_REFERENCE);

            // Parse the references to table and columns
            parseReferences(tokens, constraintNode);

            parseConstraintAttributes(tokens, constraintNode);

            consumeComment(tokens);

        } else if (tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "CHECK")) {
            // CONSTRAINT zipchk CHECK (char_length(zipcode) = 5);
            tokens.consume("CONSTRAINT"); // CONSTRAINT
            String ck_name = parseName(tokens); // NAME
            tokens.consume("CHECK"); // CHECK

            AstNode constraintNode = nodeFactory().node(ck_name, tableNode, mixinType);
            constraintNode.setProperty(CONSTRAINT_TYPE, CONSTRAINT_C);

            String clause = consumeParenBoundedTokens(tokens, true);
            constraintNode.setProperty(CHECK_SEARCH_CONDITION, clause);
        }

    }

    /**
     * Parses the attributes associated with any in-line column constraint definition or a table constrain definition.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param constraintNode
     * @throws ParsingException
     */
    protected void parseConstraintAttributes( DdlTokenStream tokens,
                                              AstNode constraintNode ) throws ParsingException {
        assert tokens != null;
        assert constraintNode != null;

        // Now we need to check for constraint attributes:

        // <constraint attributes> ::=
        // <constraint check time> [ [ NOT ] DEFERRABLE ]
        // | [ NOT ] DEFERRABLE [ <constraint check time> ]
        //
        // <constraint check time> ::=
        // INITIALLY DEFERRED
        // | INITIALLY IMMEDIATE

        // EXAMPLE : foreign key (contact_id) references contact (contact_id) on delete cascade INITIALLY DEFERRED,
        if (tokens.canConsume("INITIALLY", "DEFERRED")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "INITIALLY DEFERRED");
        }
        if (tokens.canConsume("INITIALLY", "IMMEDIATE")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "INITIALLY IMMEDIATE");
        }
        if (tokens.canConsume("NOT", "DEFERRABLE")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "NOT DEFERRABLE");
        }
        if (tokens.canConsume("DEFERRABLE")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "DEFERRABLE");
        }
        if (tokens.canConsume("INITIALLY", "DEFERRED")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "INITIALLY DEFERRED");
        }
        if (tokens.canConsume("INITIALLY", "IMMEDIATE")) {
            AstNode attrNode = nodeFactory().node("CONSTRAINT_ATTRIBUTE", constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
            attrNode.setProperty(PROPERTY_VALUE, "INITIALLY IMMEDIATE");
        }
    }

    protected void parseReferences( DdlTokenStream tokens,
                                    AstNode constraintNode ) throws ParsingException {
        assert tokens != null;
        assert constraintNode != null;

        if (tokens.matches("REFERENCES")) {
            tokens.consume("REFERENCES");
            // 'REFERENCES' referencedTableAndColumns matchType? referentialTriggeredAction?;
            String tableName = parseName(tokens);

            nodeFactory().node(tableName, constraintNode, TYPE_TABLE_REFERENCE);

            parseColumnNameList(tokens, constraintNode, TYPE_FK_COLUMN_REFERENCE);

            tokens.canConsume("MATCH", "FULL");
            tokens.canConsume("MATCH", "PARTIAL");

            //	
            // referentialTriggeredAction : (updateRule deleteRule?) | (deleteRule updateRule?);
            //
            // deleteRule : 'ON' 'DELETE' referencialAction;
            //	
            // updateRule : 'ON' 'UPDATE' referencialAction;
            //
            // referencialAction
            // : cascadeOption | setNullOption | setDefaultOption | noActionOption
            // ;
            //    		
            // cascadeOption : 'CASCADE';
            // setNullOption : 'SET' 'NULL';
            // setDefaultOption : 'SET' 'DEFAULT';
            // noActionOption : 'NO' 'ACTION';
            // nowOption : 'NOW' '(' ')' ;

            // Could be one or both, so check more than once.
            while (tokens.canConsume("ON", "UPDATE") || tokens.canConsume("ON", "DELETE")) {

                if (tokens.matches("CASCADE") || tokens.matches("NOW()")) {
                    tokens.consume();
                } else if (tokens.matches("SET", "NULL")) {
                    tokens.consume("SET", "NULL");
                } else if (tokens.matches("SET", "DEFAULT")) {
                    tokens.consume("SET", "DEFAULT");
                } else if (tokens.matches("NO", "ACTION")) {
                    tokens.consume("NO", "ACTION");
                } else {
                    System.out.println(" ERROR:   ColumnDefinition REFERENCES has NO REFERENCIAL ACTION.");
                }
            }
        }
    }

    // ===========================================================================================================================
    // PARSING CREATE VIEW
    // ===========================================================================================================================

    /**
     * Parses DDL CREATE VIEW {@link AstNode} basedregisterStatementStartPhrase on SQL 92 specifications. Initial implementation
     * here does not parse the statement in detail.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the Create View node
     * @throws ParsingException
     */
    protected AstNode parseCreateViewStatement( DdlTokenStream tokens,
                                                AstNode parentNode ) throws ParsingException {
        assert tokens != null;
        assert parentNode != null;

        markStartOfStatement(tokens);
        // <view definition> ::=
        // CREATE VIEW <table name> [ <left paren> <view column list><right paren> ]
        // AS <query expression>
        // [ WITH [ <levels clause> ] CHECK OPTION ]
        // <levels clause> ::=
        // CASCADED | LOCAL

        // NOTE: the query expression along with the CHECK OPTION clause require no SQL statement terminator.
        // So the CHECK OPTION clause will NOT

        String stmtType = "CREATE";
        tokens.consume("CREATE");
        if (tokens.canConsume("OR", "REPLACE")) {
            stmtType = stmtType + SPACE + "OR REPLACE";
        }
        tokens.consume("VIEW");
        stmtType = stmtType + SPACE + "VIEW";

        String name = parseName(tokens);

        AstNode createViewNode = nodeFactory().node(name, parentNode, TYPE_CREATE_VIEW_STATEMENT);

        // CONSUME COLUMNS
        parseColumnNameList(tokens, createViewNode, TYPE_COLUMN_REFERENCE);

        tokens.consume("AS");
        
        String queryExpression = parseUntilTerminator(tokens);

        createViewNode.setProperty(CREATE_VIEW_QUERY_EXPRESSION, queryExpression);

        markEndOfStatement(tokens, createViewNode);

        return createViewNode;
    }

    // ===========================================================================================================================
    // PARSING CREATE SCHEMA
    // ===========================================================================================================================

    /**
     * Parses DDL CREATE SCHEMA {@link AstNode} based on SQL 92 specifications. Initial implementation here does not parse the
     * statement in detail.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed schema node
     * @throws ParsingException
     */
    protected AstNode parseCreateSchemaStatement( DdlTokenStream tokens,
                                                  AstNode parentNode ) throws ParsingException {
        markStartOfStatement(tokens);

        AstNode schemaNode = null;

        String authorizationIdentifier = null;
        String schemaName = null;

        tokens.consume("CREATE", "SCHEMA");

        if (tokens.canConsume("AUTHORIZATION")) {
            authorizationIdentifier = tokens.consume();
        } else {
            schemaName = parseName(tokens);
            if (tokens.canConsume("AUTHORIZATION")) {
                authorizationIdentifier = parseName(tokens);
            }
        }
        // Must have one or the other or both
        assert authorizationIdentifier != null || schemaName != null;

        if (schemaName != null) {
            schemaNode = nodeFactory().node(schemaName, parentNode, TYPE_CREATE_SCHEMA_STATEMENT);
        } else {
            schemaNode = nodeFactory().node(authorizationIdentifier, parentNode, TYPE_CREATE_SCHEMA_STATEMENT);
        }

        if (tokens.canConsume("DEFAULT", "CHARACTER", "SET")) {
            // consume name
            parseName(tokens);
        }

        markEndOfStatement(tokens, schemaNode);

        return schemaNode;
    }

    // ===========================================================================================================================
    // PARSING CREATE XXXXX (Typed Statements)
    // ===========================================================================================================================

    /**
     * Utility method to parse a statement that can be ignored. The value returned in the generic {@link AstNode} will contain all
     * text between starting token and either the terminator (if defined) or the next statement start token. NOTE: This method
     * does NOT mark and add consumed fragment to parent node.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param name
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return the parsed generic {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseIgnorableStatement( DdlTokenStream tokens,
                                               String name,
                                               AstNode parentNode ) {

        AstNode node = nodeFactory().node(name, parentNode, TYPE_STATEMENT);

        parseUntilTerminator(tokens);

        // System.out.println(" >>> FOUND [" + stmt.getType() +"] STATEMENT TOKEN. IGNORING");
        return node;
    }

    /**
     * Utility method to parse a statement that can be ignored. The value returned in the generic {@link AstNode} will contain all
     * text between starting token and either the terminator (if defined) or the next statement start token. NOTE: This method
     * does NOT mark and add consumed fragment to parent node.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param name
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @param mixinType
     * @return the parsed generic {@link AstNode}
     * @throws ParsingException
     */
    protected AstNode parseIgnorableStatement( DdlTokenStream tokens,
                                               String name,
                                               AstNode parentNode,
                                               Name mixinType ) {
        assert tokens != null;
        assert name != null;
        assert parentNode != null;
        assert mixinType != null;

        AstNode node = nodeFactory().node(name, parentNode, mixinType);

        parseUntilTerminator(tokens);

        return node;
    }

    /**
     * Utility method to parse a generic statement given a start phrase and statement mixin type.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param stmt_start_phrase the string array statement start phrase
     * @param parentNode the parent of the newly created node.
     * @param mixinType the mixin type of the newly created statement node
     * @return the new node
     */
    protected AstNode parseStatement( DdlTokenStream tokens,
                                      String[] stmt_start_phrase,
                                      AstNode parentNode,
                                      Name mixinType ) {
        assert tokens != null;
        assert stmt_start_phrase != null;
        assert parentNode != null;
        assert mixinType != null;

        markStartOfStatement(tokens);
        tokens.consume(stmt_start_phrase);
        AstNode result = parseIgnorableStatement(tokens, getStatementTypeName(stmt_start_phrase), parentNode, mixinType);
        markEndOfStatement(tokens, result);

        return result;
    }

    /**
     * Constructs a terminator AstNode as child of root node
     * 
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return terminator node
     */
    public final AstNode unknownTerminatedNode( AstNode parentNode ) {
        return nodeFactory.node("unknownStatement", parentNode, StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT);
    }

    /**
     * Constructs a terminator AstNode as child of root node
     * 
     * @param parentNode the parent {@link AstNode} node; may not be null
     * @return terminator node
     */
    public final AstNode missingTerminatorNode( AstNode parentNode ) {
        return nodeFactory.node("missingTerminator", parentNode, StandardDdlLexicon.TYPE_MISSING_TERMINATOR);
    }

    public final boolean isMissingTerminatorNode( AstNode node ) {
        return node.getName().getString().equals(MISSING_TERMINATOR_NODE_LITERAL)
               && nodeFactory().hasMixinType(node, TYPE_MISSING_TERMINATOR);
    }

    public final boolean isValidSchemaChild( AstNode node ) {
        Name[] schemaChildMixins = getValidSchemaChildTypes();
        for (Object mixin : node.getProperty(JcrLexicon.MIXIN_TYPES).getValuesAsArray()) {
            if (mixin instanceof Name) {
                for (Name nextType : schemaChildMixins) {
                    if (nextType.equals(mixin)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public final boolean setAsSchemaChildNode( AstNode statementNode,
                                               boolean stmtIsMissingTerminator ) {

        if (!isValidSchemaChild(statementNode)) {
            return false;
        }

        // Because we are setting the schema children on the fly we can assume that if we are under a schema with children, then
        // the schema should be followed by a missing terminator node. So we just check the previous 2 nodes.

        List<AstNode> children = getRootNode().getChildren();

        if (children.size() > 2) {
            AstNode previousNode = children.get(children.size() - 2);
            if (nodeFactory().hasMixinType(previousNode, TYPE_MISSING_TERMINATOR)) {
                AstNode theSchemaNode = children.get(children.size() - 3);

                // If the last child of a schema is missing terminator, then the schema isn't complete.
                // If it is NOT a missing terminator, we aren't under a schema node anymore.
                if (theSchemaNode.getChildCount() == 0
                    || nodeFactory().hasMixinType(theSchemaNode.getLastChild(), TYPE_MISSING_TERMINATOR)) {
                    if (nodeFactory().hasMixinType(theSchemaNode, TYPE_CREATE_SCHEMA_STATEMENT)) {
                        statementNode.setParent(theSchemaNode);
                        if (stmtIsMissingTerminator) {
                            missingTerminatorNode(theSchemaNode);
                        }
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Returns current terminator
     * 
     * @return terminator string value
     */
    protected String getTerminator() {
        return this.terminator;
    }

    /**
     * @param terminator the string value used as the statement terminator for the ddl dialect
     * @return if terminator was changed or not
     */
    protected boolean setTerminator( String terminator ) {
        CheckArg.isNotNull(terminator, "terminator");
        if (this.terminator.equalsIgnoreCase(terminator)) {
            return false;
        }
        this.terminator = terminator;
        return true;
    }

    /**
     * Adds the provided key words array to the list of registered key words.
     * 
     * @param keywords
     */
    public void registerKeyWords( String[] keywords ) {
        tokens.registerKeyWords(keywords);
    }

    /**
     * Adds the provided key words list to the list of registered key words.
     * 
     * @param keywords
     */
    public void registerKeyWords( List<String> keywords ) {
        tokens.registerKeyWords(keywords);
    }

    /**
     * Adds the provided statement start phrase to the list of registered phrases.
     * 
     * @param phrase
     */
    public void registerStatementStartPhrase( String[] phrase ) {
        tokens.registerStatementStartPhrase(phrase);
    }

    /**
     * Adds the provided statement start phrases to the list of registered phrases.
     * 
     * @param phrases
     */
    public void registerStatementStartPhrase( String[][] phrases ) {
        tokens.registerStatementStartPhrase(phrases);
    }

    /**
     * Adds the provided statement start phrases to the list of registered phrases.
     * 
     * @param phrases
     */
    private void registerSchemaChildTypes( Name[] phrases ) {
        validSchemaChildTypes.clear();
        for (Name phrase : phrases) {
            validSchemaChildTypes.add(phrase);
        }
    }

    protected Name[] getValidSchemaChildTypes() {
        return VALID_SCHEMA_CHILD_TYPES;
    }

    /**
     * Checks if next token is of type comment.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return true if next token is a comment.
     * @throws ParsingException
     */
    protected boolean isComment( DdlTokenStream tokens ) throws ParsingException {
        return tokens.matches(DdlTokenizer.COMMENT);
    }

    /**
     * Consumes an an end-of-line comment or in-line comment
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @throws ParsingException
     */
    protected void consumeComment( DdlTokenStream tokens ) throws ParsingException {
        tokens.canConsume(DdlTokenizer.COMMENT);
    }

    /**
     * This utility method provides this parser the ability to distinguish between a CreateTable Constraint and a ColumnDefinition
     * Definition which are the only two statement segment types allowed within the CREATE TABLE parenthesis ( xxxxxx );
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return is table constraint
     * @throws ParsingException
     */
    protected boolean isTableConstraint( DdlTokenStream tokens ) throws ParsingException {
        boolean result = false;

        if ((tokens.matches("PRIMARY", "KEY")) || (tokens.matches("FOREIGN", "KEY")) || (tokens.matches("UNIQUE"))) {
            result = true;
        } else if (tokens.matches("CONSTRAINT")) {
            if (tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "UNIQUE")
                || tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "PRIMARY", "KEY")
                || tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "FOREIGN", "KEY")
                || tokens.matches("CONSTRAINT", DdlTokenStream.ANY_VALUE, "CHECK")) {
                result = true;
            }
        }

        return result;
    }

    /**
     * This utility method provides this parser the ability to distinguish between a CreateTable Constrain and a ColumnDefinition
     * Definition which are the only two statement segment types allowed within the CREATE TABLE parenthesis ( xxxxxx );
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return is column definition start phrase
     * @throws ParsingException
     */
    protected boolean isColumnDefinitionStart( DdlTokenStream tokens ) throws ParsingException {
        boolean result = false;

        if (isTableConstraint(tokens)) {
            result = false;
        } else {
            for (String dTypeStartWord : getDataTypeStartWords()) {
                result = (tokens.matches(DdlTokenStream.ANY_VALUE, dTypeStartWord) || tokens.matches("COLUMN",
                                                                                                     DdlTokenStream.ANY_VALUE,
                                                                                                     dTypeStartWord));
                if (result) {
                    break;
                }
            }

        }

        return result;
    }

    /**
     * Returns a list of data type start words which can be used to help identify a column definition sub-statement.
     * 
     * @return list of data type start words
     */
    protected List<String> getDataTypeStartWords() {
        if (allDataTypeStartWords == null) {
            allDataTypeStartWords = new ArrayList<String>();
            allDataTypeStartWords.addAll(DataTypes.DATATYPE_START_WORDS);
            allDataTypeStartWords.addAll(getCustomDataTypeStartWords());
        }
        return allDataTypeStartWords;
    }

    /**
     * Returns a list of custom data type start words which can be used to help identify a column definition sub-statement.
     * Sub-classes should override this method to contribute DB-specific data types.
     * 
     * @return list of data type start words
     */
    protected List<String> getCustomDataTypeStartWords() {
        return Collections.emptyList();
    }

    /**
     * Method to parse fully qualified schema, table and column names that are defined with '.' separator and optionally bracketed
     * with square brackets Example: partsSchema.supplier Example: [partsSchema].[supplier]
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return the parsed name
     */
    protected String parseName( DdlTokenStream tokens ) {
        // Basically we want to construct a name that could have the form:
        // [schemaName].[tableName].[columnName]
        // NOTE: "[]" brackets are optional
        StringBuffer sb = new StringBuffer();

        if (tokens.matches('[')) {
            // We have the bracketed case, so assume all brackets
            while (true) {

                tokens.consume('['); // [ bracket
                sb.append(consumeIdentifier(tokens)); // name
                tokens.consume(']'); // ] bracket
                if (tokens.matches('.')) {
                    sb.append(tokens.consume()); // '.'
                } else {
                    break;
                }
            }
        } else {

            // We have the NON-bracketed case, so assume all brackets
            while (true) {

                sb.append(consumeIdentifier(tokens)); // name

                if (tokens.matches('.')) {
                    sb.append(tokens.consume()); // '.'
                } else {
                    break;
                }

            }
        }

        return sb.toString();
    }

    /**
     * Consumes an token identifier which can be of the form of a simple string or a double-quoted string.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return the identifier
     * @throws ParsingException
     */
    protected String consumeIdentifier( DdlTokenStream tokens ) throws ParsingException {
        String value = tokens.consume();
        // This may surrounded by quotes, so remove them ...
        if (value.charAt(0) == '"') {
            int length = value.length();
            // Check for the end quote ...
            value = value.substring(1, length - 1); // not complete!!
        }
        // TODO: Handle warnings elegantly
        // else {
        // // Not quoted, so check for reserved words ...
        // if (tokens.isKeyWord(value)) {
        // // Record warning ...
        // System.out.println("  WARNING:  Identifier [" + value + "] is a SQL 92 Reserved Word");
        // }
        // }
        return value;
    }

    /**
     * Utility method to determine if next token is a terminator.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return is terminator token
     * @throws ParsingException
     */
    protected boolean isTerminator( DdlTokenStream tokens ) throws ParsingException {
        boolean result = tokens.matches(getTerminator());

        return result;
    }

    protected void parseColumnNameList( DdlTokenStream tokens,
                                        AstNode parentNode,
                                        Name referenceType ) {
        // CONSUME COLUMNS
        List<String> columnNameList = new ArrayList<String>();
        if (tokens.matches(L_PAREN)) {
            tokens.consume(L_PAREN);
            columnNameList = parseColumnNameList(tokens);
            tokens.consume(R_PAREN);
        }

        for (String columnName : columnNameList) {
            nodeFactory().node(columnName, parentNode, referenceType);
        }
    }

    /**
     * Parses a comma separated list of column names.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return list of column names.
     * @throws ParsingException
     */
    protected List<String> parseColumnNameList( DdlTokenStream tokens ) throws ParsingException {
        List<String> columnNames = new LinkedList<String>();

        while (true) {
            columnNames.add(parseName(tokens));
            if (!tokens.canConsume(COMMA)) {
                break;
            }
        }

        return columnNames;
    }

    /**
     * Utility method which parses tokens until a terminator is found, another statement is identified or there are no more
     * tokens.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return the parsed string
     * @throws ParsingException
     */
    protected String parseUntilTerminator( DdlTokenStream tokens ) throws ParsingException {
        StringBuffer sb = new StringBuffer();
        if (doUseTerminator()) {
            boolean lastTokenWasPeriod = false;
            while (tokens.hasNext() && !tokens.matches(DdlTokenizer.STATEMENT_KEY) && !isTerminator(tokens)) {
                String thisToken = tokens.consume();
                boolean thisTokenIsPeriod = thisToken.equals(PERIOD);
                boolean thisTokenIsComma = thisToken.equals(COMMA);
                if (lastTokenWasPeriod || thisTokenIsPeriod || thisTokenIsComma) {
                    sb.append(thisToken);
                } else {
                    sb.append(SPACE).append(thisToken);
                }
                if (thisTokenIsPeriod) {
                    lastTokenWasPeriod = true;
                } else {
                    lastTokenWasPeriod = false;
                }
            }
        } else {
            // parse until next statement
            boolean lastTokenWasPeriod = false;
            while (tokens.hasNext() && !tokens.matches(DdlTokenizer.STATEMENT_KEY)) {
                String thisToken = tokens.consume();
                boolean thisTokenIsPeriod = thisToken.equals(PERIOD);
                boolean thisTokenIsComma = thisToken.equals(COMMA);
                if (lastTokenWasPeriod || thisTokenIsPeriod || thisTokenIsComma) {
                    sb.append(thisToken);
                } else {
                    sb.append(SPACE).append(thisToken);
                }
                if (thisTokenIsPeriod) {
                    lastTokenWasPeriod = true;
                } else {
                    lastTokenWasPeriod = false;
                }
            }
        }

        return sb.toString();
    }

    /**
     * Utility method which parses tokens until a terminator is found or there are no more tokens. This method differs from
     * parseUntilTermintor() in that it ignores embedded statements. This method can be used for parsers that have statements
     * which can embed statements that should not be parsed.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return the parsed string
     * @throws ParsingException
     */
    protected String parseUntilTerminatorIgnoreEmbeddedStatements( DdlTokenStream tokens ) throws ParsingException {
        StringBuffer sb = new StringBuffer();

        boolean lastTokenWasPeriod = false;
        while (tokens.hasNext() && !isTerminator(tokens)) {
            String thisToken = tokens.consume();
            boolean thisTokenIsPeriod = thisToken.equals(PERIOD);
            boolean thisTokenIsComma = thisToken.equals(COMMA);
            if (lastTokenWasPeriod || thisTokenIsPeriod || thisTokenIsComma) {
                sb.append(thisToken);
            } else {
                sb.append(SPACE).append(thisToken);
            }
            if (thisTokenIsPeriod) {
                lastTokenWasPeriod = true;
            } else {
                lastTokenWasPeriod = false;
            }
        }

        return sb.toString();
    }

    /**
     * Utility method which parses tokens until a semicolon is found or there are no more tokens.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @return the parsed string
     * @throws ParsingException
     */
    protected String parseUntilSemiColon( DdlTokenStream tokens ) throws ParsingException {
        StringBuffer sb = new StringBuffer();

        boolean lastTokenWasPeriod = false;
        while (tokens.hasNext() && !tokens.matches(SEMICOLON)) {
            String thisToken = tokens.consume();
            boolean thisTokenIsPeriod = thisToken.equals(PERIOD);
            boolean thisTokenIsComma = thisToken.equals(COMMA);
            if (lastTokenWasPeriod || thisTokenIsPeriod || thisTokenIsComma) {
                sb.append(thisToken);
            } else {
                sb.append(SPACE).append(thisToken);
            }
            if (thisTokenIsPeriod) {
                lastTokenWasPeriod = true;
            } else {
                lastTokenWasPeriod = false;
            }
        }

        return sb.toString();
    }

    protected String parseUntilCommaOrTerminator( DdlTokenStream tokens ) throws ParsingException {
        StringBuffer sb = new StringBuffer();
        if (doUseTerminator()) {
            while (tokens.hasNext() && !tokens.matches(DdlTokenizer.STATEMENT_KEY) && !isTerminator(tokens)
                   && !tokens.matches(COMMA)) {
                sb.append(SPACE).append(tokens.consume());
            }
        } else {
            // parse until next statement
            while (tokens.hasNext() && !tokens.matches(DdlTokenizer.STATEMENT_KEY) && !tokens.matches(COMMA)) {
                sb.append(SPACE).append(tokens.consume());
            }
        }

        return sb.toString();
    }

    /**
     * Returns if parser is using statement terminator or not.
     * 
     * @return value of useTerminator flag.
     */
    public boolean doUseTerminator() {
        return useTerminator;
    }

    /**
     * Sets the value of the use terminator flag for the parser. If TRUE, then all statements are expected to be terminated by a
     * terminator. The default terminator ";" can be overridden by setting the value using setTerminator() method.
     * 
     * @param useTerminator
     */
    public void setDoUseTerminator( boolean useTerminator ) {
        this.useTerminator = useTerminator;
    }

    public String getStatementTypeName( String[] stmtPhrase ) {
        StringBuffer sb = new StringBuffer(100);
        for (int i = 0; i < stmtPhrase.length; i++) {
            if (i == 0) {
                sb.append(stmtPhrase[0]);
            } else {
                sb.append(SPACE).append(stmtPhrase[i]);
            }
        }

        return sb.toString();
    }

    /**
     * Parses the default clause for a column and sets appropriate properties on the column node.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param columnNode the column node which may contain a default clause; may not be null
     * @return true if default clause was found and parsed, otherwise false
     * @throws ParsingException
     */
    protected boolean parseDefaultClause( DdlTokenStream tokens,
                                          AstNode columnNode ) throws ParsingException {

        assert tokens != null;
        assert columnNode != null;

        // defaultClause
        // : defaultOption
        // ;
        // defaultOption : <literal> | datetimeValueFunction
        // | USER | CURRENT_USER | SESSION_USER | SYSTEM_USER | NULL;
        //
        // <datetime value function> ::=
        // <current date value function>
        // | <current time value function>
        // | <current timestamp value function>
        //
        // <current date value function> ::= CURRENT_DATE
        //
        // <current time value function> ::=
        // CURRENT_TIME [ <left paren> <time precision> <right paren> ]
        //
        // <current timestamp value function> ::=
        // CURRENT_TIMESTAMP [ <left paren> <timestamp precision> <right paren> ]

        String defaultValue = "";

        if (tokens.canConsume("DEFAULT")) {

            int optionID = -1;
            int precision = -1;

            if (tokens.canConsume("CURRENT_DATE")) {

                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_DATE";
            } else if (tokens.canConsume("CURRENT_TIME")) {
                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_TIME";
                if (tokens.canConsume(L_PAREN)) {
                    // EXPECT INTEGER
                    precision = integer(tokens.consume());
                    tokens.canConsume(R_PAREN);
                }
            } else if (tokens.canConsume("CURRENT_TIMESTAMP")) {
                optionID = DEFAULT_ID_DATETIME;
                defaultValue = "CURRENT_TIMESTAMP";
                if (tokens.canConsume(L_PAREN)) {
                    // EXPECT INTEGER
                    precision = integer(tokens.consume());
                    tokens.canConsume(R_PAREN);
                }
            } else if (tokens.canConsume("USER")) {
                optionID = DEFAULT_ID_USER;
                defaultValue = "USER";
            } else if (tokens.canConsume("CURRENT_USER")) {
                optionID = DEFAULT_ID_CURRENT_USER;
                defaultValue = "CURRENT_USER";
            } else if (tokens.canConsume("SESSION_USER")) {
                optionID = DEFAULT_ID_SESSION_USER;
                defaultValue = "SESSION_USER";
            } else if (tokens.canConsume("SYSTEM_USER")) {
                optionID = DEFAULT_ID_SYSTEM_USER;
                defaultValue = "SYSTEM_USER";
            } else if (tokens.canConsume("NULL")) {
                optionID = DEFAULT_ID_NULL;
                defaultValue = "NULL";
            } else if (tokens.canConsume(L_PAREN)) {
                optionID = DEFAULT_ID_LITERAL;
                while (!tokens.canConsume(R_PAREN)) {
                    defaultValue = defaultValue + tokens.consume();
                }
            } else {
                optionID = DEFAULT_ID_LITERAL;
                // Assume default was EMPTY or ''
                defaultValue = tokens.consume();
                // NOTE: default value could be a Real number as well as an integer, so
                // 1000.00 is valid
                if (tokens.canConsume(".")) {
                    defaultValue = defaultValue + '.' + tokens.consume();
                }
            }

            columnNode.setProperty(DEFAULT_OPTION, optionID);
            columnNode.setProperty(DEFAULT_VALUE, defaultValue);
            if (precision > -1) {
                columnNode.setProperty(DEFAULT_PRECISION, precision);
            }
            return true;
        }

        return false;
    }

    /**
     * Parses the default clause for a column and sets appropriate properties on the column node.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param columnNode the column node which may contain a collate clause; may not be null
     * @return true if collate clause was found and parsed else return false.
     * @throws ParsingException
     */
    protected boolean parseCollateClause( DdlTokenStream tokens,
                                          AstNode columnNode ) throws ParsingException {
        assert tokens != null;
        assert columnNode != null;

        // an option in the CREATE DOMAIN definition
        //
        // <collate clause> ::= COLLATE <collation name>

        if (tokens.matches("COLLATE")) {
            tokens.consume("COLLATE");
            String collationName = parseName(tokens);
            columnNode.setProperty(COLLATION_NAME, collationName);
            return true;
        }

        return false;
    }

    /**
     * Returns the integer value of the input string. Handles both straight integer string or complex KMG (CLOB or BLOB) value.
     * Throws {@link NumberFormatException} if a valid integer is not found.
     * 
     * @param value the string to be parsed; may not be null and length must be > 0;
     * @return integer value
     */
    protected int integer( String value ) {
        assert value != null;
        assert value.length() > 0;

        return new BigInteger(value).intValue();
    }

    public final Position getCurrentMarkedPosition() {
        return currentMarkedPosition;
    }

    /**
     * Marks the token stream with the current position to help track statement scope within the original input string.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     */
    public final void markStartOfStatement( DdlTokenStream tokens ) {
        tokens.mark();
        currentMarkedPosition = tokens.nextPosition();
    }

    /**
     * Marks the end of a statement by consuming the terminator (if exists). If it does not exist, a missing terminator node may
     * be added. If the resulting statement node is a valid child node type for a schema, the child node may be re-parented to the
     * schema if the schema is still parentable. Each resulting statement node is tagged with the enclosing source expression,
     * starting line number and column number from the file content as well as a starting character index from that same content.
     * 
     * @param tokens the {@link DdlTokenStream} representing the tokenized DDL content; may not be null
     * @param statementNode
     */
    public final void markEndOfStatement( DdlTokenStream tokens,
                                          AstNode statementNode ) {
        if (!tokens.canConsume(getTerminator())) {
            // System.out.println("  WARNING:  Terminator NOT FOUND");

            // Check previous until
            // 1) find two sequential nodes that are not missing terminator nodes
            // 2) the node before the missing terminator is a valid schema child and
            // 3) we find a schema node that is ALSO missing a terminator BEFORE we find an invalid schema child OR a terminated
            // node.

            if (!setAsSchemaChildNode(statementNode, true)) {
                missingTerminatorNode(getRootNode()); // Construct missing terminator node
            }
        } else {
            setAsSchemaChildNode(statementNode, false);
        }

        String source = tokens.getMarkedContent().trim();
        statementNode.setProperty(DDL_EXPRESSION, source);
        statementNode.setProperty(DDL_START_LINE_NUMBER, currentMarkedPosition.getLine());
        statementNode.setProperty(DDL_START_CHAR_INDEX, currentMarkedPosition.getIndexInContent());
        statementNode.setProperty(DDL_START_COLUMN_NUMBER, currentMarkedPosition.getColumn());

        testPrint("== >> SOURCE:\n" + source + "\n");
    }

    protected void testPrint( String str ) {
        if (isTestMode()) {
            System.out.println(str);
        }
    }

    /**
     * @return testMode
     */
    public boolean isTestMode() {
        return testMode;
    }

    /**
     * @param testMode Sets testMode to the specified value.
     */
    public void setTestMode( boolean testMode ) {
        this.testMode = testMode;
    }

    public String getId() {
        return this.parserId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.sequencer.ddl.DdlParser#isType(String)
     */
    public boolean isType( String ddl ) {
        String trimmedDDL = ddl.trim();
        int endIndex = 400;
        if (trimmedDDL.length() < endIndex) {
            endIndex = trimmedDDL.length() - 1;
        }
        DdlTokenStream tokens = new DdlTokenStream(ddl.substring(0, endIndex), DdlTokenStream.ddlTokenizer(false), false);
        tokens.start();
        if (tokens.matches("PARSER_ID", "=", getId())) {
            return true;
        }

        return false;
    }
}