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
package org.jboss.dna.test.integration;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import org.jboss.dna.connector.svn.SVNRepositorySource;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.jboss.dna.jcr.JcrRepository.Option;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class SvnAndJcrIntegrationTest {
    private JcrEngine engine;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        final String repositoryUrl = "http://anonsvn.jboss.org/repos/dna/";
        final String[] predefinedWorkspaceNames = {repositoryUrl + "trunk", repositoryUrl + "tags", repositoryUrl + "branches"};
        final String svnRepositorySource = "svnRepositorySource";
        final String repositoryName = "svnRepository";
        final JcrConfiguration configuration = new JcrConfiguration();
        configuration.repositorySource(svnRepositorySource)
                     .usingClass(SVNRepositorySource.class)
                     .setProperty("password", "")
                     .setProperty("username", "anonymous")
                     .setProperty("repositoryRootURL", repositoryUrl)
                     .setProperty("predefinedWorkspaceNames", predefinedWorkspaceNames)
                     .setProperty("directoryForDefaultWorkspace", predefinedWorkspaceNames[0])
                     .setProperty("creatingWorkspacesAllowed", false);

        configuration.repository(repositoryName).setSource(svnRepositorySource).setOption(Option.READ_DEPTH, "1");
        configuration.save();
        this.engine = configuration.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new SecurityContextCredentials(new MyCustomSecurityContext()));

    }

    @After
    public void afterEach() throws Exception {
        if (this.session != null) {
            this.session.logout();
        }
        if (this.engine != null) {
            this.engine.shutdown();
        }
    }

    @Test
    public void shouldIterateOverChildrenOfRoot() throws Exception {
        NodeIterator nodeIterator = this.session.getRootNode().getNodes();

        while (nodeIterator.hasNext()) {
            System.out.println(nodeIterator.nextNode().getPath());
        }
        assertThat(this.session.getRootNode().getNode("dna-graph"), is(notNullValue()));
    }

    protected class MyCustomSecurityContext implements SecurityContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return "Fred";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#hasRole(java.lang.String)
         */
        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }
}