package graph;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provides global access to a singleton TopicManager for managing topics in the computational graph.
 * 
 * <p>This class implements the Singleton pattern to make sure there is one TopicManager
 * instance throughout the application life cycle. It is the central manager for all
 * topics in the computational graph system.</p>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // Get the singleton instance
 * TopicManager tm = TopicManagerSingleton.get();
 * 
 * // Create or retrieve topics
 * Topic inputTopic = tm.getTopic("UserInput");
 * Topic outputTopic = tm.getTopic("Results");
 * 
 * // Use topics with agents
 * inputTopic.subscribe(myAgent);
 * outputTopic.addPublisher(myAgent);
 * }</pre>
 * 
 * @author Ariella Noy
 * @version 1.0
 * @since 1.0
 * 
 * @see TopicManager
 * @see Topic
 */
public class TopicManagerSingleton {

	/**
	 * The singleton TopicManager that manages all topics in the computational graph.
	 * 
	 * <p>This inner class provides thread-safe topic management with automatic topic
	 * creation and centralized access to all topics in the system.</p>
	 * 
	 * <p><strong>Thread Safety:</strong> Uses ConcurrentHashMap for thread-safe operations.</p>
	 */
	public static class TopicManager{
		
		/** Thread-safe map storing all topics by name */
		private final ConcurrentHashMap<String, Topic> topics;
		
		/** The singleton instance of TopicManager */
		private static final TopicManager instance = new TopicManager();

		/**
		 * Private constructor to enforce singleton pattern.
		 * Initializes the internal topic storage.
		 */
		private TopicManager() {
			this.topics = new ConcurrentHashMap<>();
		}
		
		/**
		 * Retrieves or creates a topic with the specified name.
		 * 
		 * <p>If a topic with the given name already exists, it is returned.
		 * If no such topic exists, a new one is created, stored, and returned.</p>
		 * 
		 * @param name the name of the topic to retrieve or create
		 * @return the existing or newly created topic
		 * 
		 * @example
		 * <pre>{@code
		 * TopicManager tm = TopicManagerSingleton.get();
		 * Topic topic1 = tm.getTopic("Data"); // Creates new topic
		 * Topic topic2 = tm.getTopic("Data"); // Returns same topic
		 * assert topic1 == topic2; // true
		 * }</pre>
		 */
		public Topic getTopic(String name) {
			return topics.computeIfAbsent(name, Topic::new);
		}
		
		/**
		 * Returns a collection of all currently managed topics.
		 * 
		 * @return collection containing all topics in the system
		 */
		public Collection<Topic> getTopics() {
			return topics.values();
		}
		
		/**
		 * Removes all topics from the manager.
		 * 
		 * <p>This method clears all topics from the system. Use with caution
		 * as it will break existing agent subscriptions and publisher relationships.</p>
		 */
		public void clear(){
			topics.clear();
		}		
	}
		
	/**
	 * Returns the singleton TopicManager instance.
	 * 
	 * @return the singleton TopicManager instance
	 */
	public static TopicManager get() {
		return TopicManager.instance;
	}
}