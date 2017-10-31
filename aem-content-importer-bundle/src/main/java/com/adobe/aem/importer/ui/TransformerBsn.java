/*******************************************************************************
* Copyright (c) 2014 Adobe Systems Incorporated. All rights reserved.
*
* Licensed under the Apache License 2.0.
* http://www.apache.org/licenses/LICENSE-2.0
******************************************************************************/
package com.adobe.aem.importer.ui;

import com.adobe.cq.sightly.WCMUse;

public class TransformerBsn extends WCMUse{

	private Transformer[] list;

	@Override
	public void activate() throws Exception {

		list = new Transformer[2];

        list[0] = new Transformer();
        list[0].setName("DITA");
        list[0].setSimpleName("DITA");

        list[1] = new Transformer();
        list[1].setName("DocBook");
        list[1].setSimpleName("DocBook");
    }

	public Transformer[] getList() {
		return list;
	}

	public class Transformer {
		String name;
		String simpleName;

		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getSimpleName() {
			return simpleName;
		}
		public void setSimpleName(String simpleName) {
			this.simpleName = simpleName;
		}
	}

}
