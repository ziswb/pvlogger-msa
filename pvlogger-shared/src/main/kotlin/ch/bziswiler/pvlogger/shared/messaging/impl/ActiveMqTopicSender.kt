package ch.bziswiler.pvlogger.shared.messaging.impl

import ch.bziswiler.pvlogger.shared.messaging.MqSessionFactory
import ch.bziswiler.pvlogger.shared.messaging.TopicName
import javax.jms.Topic

/*
 * see https://codenotfound.com/jms-publish-subscribe-messaging-example-activemq-maven.html for examples
 * see https://github.com/code-not-found/jms for code
 */
open class ActiveMqTopicSender(mqSessionFactory: MqSessionFactory, name: TopicName, ephemeralMessages: Boolean = false) : ActiveMqSenderScaffold(mqSessionFactory, name, ephemeralMessages) {

    override val destination: Topic
        get() = mqSessionFactory.session.createTopic(name.value)
}
