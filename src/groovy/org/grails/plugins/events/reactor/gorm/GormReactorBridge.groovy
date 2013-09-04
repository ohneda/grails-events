package org.grails.plugins.events.reactor.gorm

import groovy.transform.CompileStatic
import org.grails.datastore.mapping.engine.event.*
import org.grails.plugins.events.reactor.api.EventsApi
import org.hibernate.event.PostDeleteEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationListener
import reactor.core.Reactor

import javax.annotation.PostConstruct
/**
 * @author Stephane Maldini
 */
@CompileStatic
class GormReactorBridge implements ApplicationListener<ApplicationEvent> {

	public final static String GORM_BRIDGE = 'gorm'

	private final EventsApi grailsEvents
	private final Set<Reactor> gormReactors = []

	final Map<Class, String> translateTable = [
			(PreInsertEvent): 'beforeInsert', (PreUpdateEvent): 'beforeUpdate', /*(PreLoadEvent): 'beforeLoad',*/
			(PreDeleteEvent): 'beforeDelete', (ValidationEvent): 'beforeValidate', (PostInsertEvent): 'afterInsert',
			(PostUpdateEvent): 'afterUpdate', (PostDeleteEvent): 'afterDelete', /*(PostLoadEvent): 'afterLoad',*/
			(SaveOrUpdateEvent): 'onSaveOrUpdate'
	]

	@Autowired
	GormReactorBridge(EventsApi grailsEvents) {
		this.grailsEvents = grailsEvents
	}

	@PostConstruct
	void init() {
		gormReactors.clear()
		for (Reactor r in grailsEvents.groovyEnvironment.reactorBuildersByExtension(GORM_BRIDGE)*.get()) {
			gormReactors << r
		}
	}

	@Override
	void onApplicationEvent(final ApplicationEvent applicationEvent) {
		if (gormReactors) {
			def topic = translateTable[applicationEvent.class]
			if (topic) {
				AbstractPersistenceEvent persistenceEvent = ((AbstractPersistenceEvent) applicationEvent)
				for (r in gormReactors) {
						r.send(topic, persistenceEvent.entityObject ?: persistenceEvent.entityAccess?.entity) {
							processCancel(persistenceEvent, it)
						}
				}
			}
		}
	}

	void processCancel(AbstractPersistenceEvent evt, returnValue) {
		if (evt != null && returnValue != null) {
			if (!returnValue) {
				evt.cancel()
			} else if (!((Boolean) returnValue)) {
				evt.cancel()
			}
		}
	}

}
