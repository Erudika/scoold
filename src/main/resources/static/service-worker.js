/*
Copyright 2015, 2019 Google Inc. All Rights Reserved.
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
*/

// Incrementing OFFLINE_VERSION will kick off the install event and force
// previously cached resources to be updated from the network.
const OFFLINE_VERSION = 4;
const CACHE_NAME = 'offline';
// Customize this with a different URL if needed.
const OFFLINE_URL = 'offline.html';

self.addEventListener('install', (event) => {
	event.waitUntil((async () => {
		const cache = await caches.open(CACHE_NAME);
		// Setting {cache: 'reload'} in the new request will ensure that the response
		// isn't fulfilled from the HTTP cache; i.e., it will be from the network.
		await cache.add(new Request(OFFLINE_URL, {cache: 'reload'})).catch(() => {
		});
	})());
});

self.addEventListener('activate', (event) => {
	// Tell the active service worker to take control of the page immediately.
	self.clients.claim();
});

//self.addEventListener('fetch', (event) => {
//	if (event.request.mode === 'navigate') {
//		if (event.request.method !== 'GET' || event.request.cache === "reload") {
//			if (event.preloadResponse) {
//				event.waitUntil(event.preloadResponse.catch(() => {}));
//			}
//			return;
//		}
//		event.respondWith((async () => {
//			try {
//				const preloadResponse = await event.preloadResponse;
//				if (preloadResponse) {
//					return preloadResponse;
//				}
//				return await fetch(event.request);
//			} catch (error) {
//				const cache = await caches.open(CACHE_NAME);
//				const cachedResponse = await cache.match(OFFLINE_URL);
//				if (cachedResponse) {
//					return cachedResponse;
//				}
//				return new Response('Offline', {status: 503, headers: {'Content-Type': 'text/plain'}});
//			}
//		})());
//	}
//
//	// If our if() condition is false, then this fetch handler won't intercept the
//	// request. If there are any other fetch handlers registered, they will get a
//	// chance to call event.respondWith(). If no fetch handlers call
//	// event.respondWith(), the request will be handled by the browser as if there
//	// were no service worker involvement.
//});