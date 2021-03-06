/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package reactor.core

import reactor.adapter.RxJava1Adapter
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.test.TestSubscriber
import rx.Observable
import rx.Single
import spock.lang.Specification

import java.util.concurrent.CompletableFuture

import static Flux.fromIterable

/**
 * @author Stephane Maldini
 */
class PublisherConversionSpec extends Specification {

  def "From and To RxJava 1 Observable"() {

	given: "Iterable publisher of 1000 to read queue"
	def obs = Observable.range(1, 1000)
	def pub = RxJava1Adapter.observableToFlux(obs)
	def queue = TestSubscriber.create()


	when: "read the queue"
	pub.subscribe(queue)

	then: "queues values correct"
	queue.awaitAndAssertNextValues(*(1..1000))
			.assertComplete()


	when: "Iterable publisher of 1000 to observable"
	pub = fromIterable(1..1000)
	obs = RxJava1Adapter.publisherToObservable(pub)
	def blocking = obs.toList()

	def v = blocking.toBlocking().single()

	then: "queues values correct"
	v[0] == 1
	v[1] == 2
	v[999] == 1000
  }

  def "From and To RxJava 1 Single"() {

	given: "Iterable publisher of 1000 to read queue"
	def obs = Single.just(1)
	def pub = Mono.from(RxJava1Adapter.singleToMono(obs))
	def queue = TestSubscriber.create()

	when: "read the queue"
	pub.subscribe(queue)

	then: "queues values correct"
	queue.awaitAndAssertNextValues(1)
			.assertComplete()


	when: "Iterable publisher of 1000 to observable"
	pub = Flux.just(1)
	def single = RxJava1Adapter.publisherToSingle(pub)
	def blocking = single.toObservable().toBlocking()

	def v = blocking.single()

	then: "queues values correct"
	v == 1
  }

  def "From and To CompletableFuture"() {

	given: "Iterable publisher of 1 to read queue"
	def obs = CompletableFuture.completedFuture([1])
	def pub = Mono.fromFuture(obs)
	def queue = TestSubscriber.create()

	when: "read the queue"
	pub.subscribe(queue)

	then: "queues values correct"
	queue.awaitAndAssertNextValues([1])
			.assertComplete()

	when: "No Values"
	obs = CompletableFuture.runAsync{ println "done" }
	pub = Mono.fromFuture(obs)
	queue = TestSubscriber.create()

	pub.subscribe(queue)

	then: "queues values correct"
	queue.await()
			.assertNoValues()
			.assertComplete()

	when: "Iterable publisher of 1 to completable future"
	def newPub = Mono.just(1)
	obs = newPub.toFuture()
	def v = obs.get()

	then: "queues values correct"
	v == 1
  }



}
