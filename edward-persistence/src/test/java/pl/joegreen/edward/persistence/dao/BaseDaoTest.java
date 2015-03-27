package pl.joegreen.edward.persistence.dao;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;

import pl.joegreen.edward.persistence.configuration.PersistenceContextConfiguration;
import pl.joegreen.edward.persistence.configuration.TestDataSourceConfig;
import pl.joegreen.edward.persistence.testkit.ModelFixtures;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { PersistenceContextConfiguration.class,
		TestDataSourceConfig.class })
@TransactionConfiguration(defaultRollback = true)
@Transactional
@Ignore
public class BaseDaoTest {

	@Autowired
	protected TaskDao taskDao;

	@Autowired
	protected ModelFixtures modelFixtures;

	@Autowired
	protected JsonDataDao jsonDataDao;

}
