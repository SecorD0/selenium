// <copyright file="ITarget.cs" company="WebDriver Committers">
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

using System.Collections.Generic;
using System.Threading.Tasks;

namespace OpenQA.Selenium.DevTools
{
    /// <summary>
    /// Interface representing a target for DevTools Protocol commands
    /// </summary>
    public interface ITarget
    {
        /// <summary>
        /// Asynchronously gets the targets available for this session.
        /// </summary>
        /// <returns>
        /// A task that represents the asynchronous operation. The task result
        /// contains the list of <see cref="TargetInfo"/> objects describing the
        /// targets available for this session.
        /// </returns>
        Task<List<TargetInfo>> GetTargets();

        /// <summary>
        /// Asynchronously attaches to a target.
        /// </summary>
        /// <param name="targetId">The ID of the target to which to attach.</param>
        /// <returns>
        /// A task representing the asynchronous attach operation. The task result contains the
        /// session ID established for commands to the target attached to.
        /// </returns>
        Task<string> AttachToTarget(string TargetId);

        /// <summary>
        /// Asynchronously sets the DevTools Protocol connection to automatically attach to new targets.
        /// </summary>
        /// <returns>A task that represents the asynchronous operation.</returns>
        Task SetAutoAttach();
    }
}
