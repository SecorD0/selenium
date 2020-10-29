// <copyright file="V84Network.cs" company="WebDriver Committers">
// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements. See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership. The SFC licenses this file
// to you under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// </copyright>

using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using OpenQA.Selenium.DevTools.V85.Fetch;
using OpenQA.Selenium.DevTools.V85.Network;

namespace OpenQA.Selenium.DevTools.V85
{
    /// <summary>
    /// Class providing functionality for manipulating network calls using version 85 of the DevTools Protocol
    /// </summary>
    public class V85Network : DevTools.Network
    {
        private FetchAdapter fetch;
        private NetworkAdapter network;

        /// <summary>
        /// Initializes a new instance of the <see cref="V85Network"/> class.
        /// </summary>
        /// <param name="network">The adapter for the Network domain.</param>
        /// <param name="fetch">The adapter for the Fetch domain.</param>
        public V85Network(NetworkAdapter network, FetchAdapter fetch)
        {
            this.network = network;
            this.fetch = fetch;
            fetch.AuthRequired += OnFetchAuthRequired;
            fetch.RequestPaused += OnFetchRequestPaused;
        }

        /// <summary>
        /// Asynchronously disables network caching.
        /// </summary>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task DisableNetworkCaching()
        {
            await network.SetCacheDisabled(new SetCacheDisabledCommandSettings() { CacheDisabled = true });
        }

        /// <summary>
        /// Asynchronously enables network caching.
        /// </summary>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task EnableNetworkCaching()
        {
            await network.SetCacheDisabled(new SetCacheDisabledCommandSettings() { CacheDisabled = false });
        }

        /// <summary>
        /// Asynchronously enables the fetch domain for all URL patterns.
        /// </summary>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task EnableFetchForAllPatterns()
        {
            await fetch.Enable(new OpenQA.Selenium.DevTools.V85.Fetch.EnableCommandSettings()
            {
                Patterns = new OpenQA.Selenium.DevTools.V85.Fetch.RequestPattern[]
                {
                    new OpenQA.Selenium.DevTools.V85.Fetch.RequestPattern() { UrlPattern = "*" }
                },
                HandleAuthRequests = true
            });
        }

        /// <summary>
        /// Asynchronously diables the fetch domain.
        /// </summary>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task DisableFetch()
        {
            await fetch.Disable();
        }

        /// <summary>
        /// Asynchronously continues an intercepted network request.
        /// </summary>
        /// <param name="requestData">The <see cref="HttpRequestData"/> of the request.</param>
        /// <param name="responseData">The <see cref="HttpResponseData"/> with which to respond to the request</param>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task ContinueRequest(HttpRequestData requestData, HttpResponseData responseData)
        {
            var commandSettings = new FulfillRequestCommandSettings()
            {
                RequestId = requestData.RequestId,
                ResponseCode = responseData.StatusCode,
            };

            if (responseData.Headers.Count > 0)
            {
                List<HeaderEntry> headers = new List<HeaderEntry>();
                foreach(KeyValuePair<string, string> headerPair in responseData.Headers)
                {
                    headers.Add(new HeaderEntry() { Name = headerPair.Key, Value = headerPair.Value });
                }

                commandSettings.ResponseHeaders = headers.ToArray();
            }

            if (!string.IsNullOrEmpty(responseData.Body))
            {
                // TODO: base64 encode?
                commandSettings.Body = responseData.Body;
            }

            await fetch.FulfillRequest(commandSettings);
        }

        /// <summary>
        /// Asynchronously contines an intercepted network call without modification.
        /// </summary>
        /// <param name="requestData">The <see cref="HttpRequestData"/> of the network call.</param>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task ContinueWithoutModification(HttpRequestData requestData)
        {
            await fetch.ContinueRequest(new ContinueRequestCommandSettings() { RequestId = requestData.RequestId });
        }

        /// <summary>
        /// Asynchronously continues an intercepted network call using authentication.
        /// </summary>
        /// <param name="requestData">The <see cref="HttpRequestData"/> of the network request.</param>
        /// <param name="userName">The user name with which to authenticate.</param>
        /// <param name="password">The password with which to authenticate.</param>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task ContinueWithAuth(HttpRequestData requestData, string userName, string password)
        {
            await fetch.ContinueWithAuth(new ContinueWithAuthCommandSettings()
            {
                AuthChallengeResponse = new OpenQA.Selenium.DevTools.V85.Fetch.AuthChallengeResponse()
                {
                    Response = OpenQA.Selenium.DevTools.V85.Fetch.AuthChallengeResponseResponseValues.ProvideCredentials,
                    Username = userName,
                    Password = password
                }
            });
        }

        /// <summary>
        /// Asynchronously cancels authorization of an intercepted network request.
        /// </summary>
        /// <param name="requestData">The <see cref="HttpRequestData"/> of the network request.</param>
        /// <returns>A task that represents the asynchronous operation.</returns>
        public override async Task CancelAuth(HttpRequestData requestData)
        {
            await fetch.ContinueWithAuth(new ContinueWithAuthCommandSettings()
            {
                AuthChallengeResponse = new OpenQA.Selenium.DevTools.V85.Fetch.AuthChallengeResponse()
                {
                    Response = OpenQA.Selenium.DevTools.V85.Fetch.AuthChallengeResponseResponseValues.CancelAuth
                }
            });
        }

        private void OnFetchAuthRequired(object sender, Fetch.AuthRequiredEventArgs e)
        {
            AuthRequiredEventArgs wrapped = new AuthRequiredEventArgs()
            {
                RequestId = e.RequestId,
                Uri = e.AuthChallenge.Origin
            };

            this.OnAuthRequired(wrapped);
        }

        private void OnFetchRequestPaused(object sender, Fetch.RequestPausedEventArgs e)
        {
            RequestPausedEventArgs wrapped = new RequestPausedEventArgs();
            if (e.ResponseErrorReason == null && e.ResponseStatusCode == null)
            {
                wrapped.RequestData = new HttpRequestData()
                {
                    RequestId = e.RequestId,
                    Method = e.Request.Method,
                    Url = e.Request.Url,
                    PostData = e.Request.PostData,
                    Headers = new Dictionary<string, string>(e.Request.Headers)
                };
            }

            this.OnRequestPaused(wrapped);
        }
    }
}
