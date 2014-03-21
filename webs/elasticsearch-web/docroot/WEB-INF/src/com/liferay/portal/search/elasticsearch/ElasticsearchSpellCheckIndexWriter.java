/**
 * Copyright (c) 2000-2013 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.portal.search.elasticsearch;

import com.liferay.portal.kernel.log.Log;
import com.liferay.portal.kernel.log.LogFactoryUtil;
import com.liferay.portal.kernel.search.BaseGenericSpellCheckIndexWriter;
import com.liferay.portal.kernel.search.BaseIndexWriter;
import com.liferay.portal.kernel.search.Document;
import com.liferay.portal.kernel.search.DocumentImpl;
import com.liferay.portal.kernel.search.Field;
import com.liferay.portal.kernel.search.SearchContext;
import com.liferay.portal.kernel.search.SearchException;
import com.liferay.portal.search.elasticsearch.connection.ElasticsearchConnectionManager;
import com.liferay.portal.search.elasticsearch.util.DocumentTypes;
import com.liferay.portal.search.elasticsearch.util.LogUtil;

import java.util.Collection;
import java.util.concurrent.Future;

import org.elasticsearch.action.deletebyquery.DeleteByQueryRequestBuilder;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.QueryBuilders;

/**
 * @author Michael C. Han
 */
public class ElasticsearchSpellCheckIndexWriter
	extends BaseGenericSpellCheckIndexWriter {

	public void afterPropertiesSet() {
		setIndexWriter(new _IndexWriterAdapter());
	}

	@Override
	public void clearQuerySuggestionDictionaryIndexes(
			SearchContext searchContext)
		throws SearchException {

		try {
			deleteIndices(searchContext, DocumentTypes.KEYWORD_QUERY);
		}
		catch (Exception e) {
			throw new SearchException("Unable to clear query suggestions", e);
		}
	}

	@Override
	public void clearSpellCheckerDictionaryIndexes(SearchContext searchContext)
		throws SearchException {

		try {
			deleteIndices(searchContext, DocumentTypes.SPELL_CHECK);
		}
		catch (Exception e) {
			throw new SearchException("Unable to to clear spell checks", e);
		}
	}

	public void setElasticsearchUpdateDocumentCommand(
		ElasticsearchUpdateDocumentCommand elasticsearchUpdateDocumentCommand) {

		_elasticsearchUpdateDocumentCommand =
			elasticsearchUpdateDocumentCommand;
	}

	protected void addDocument(
			String documentType, SearchContext searchContext, Document document)
		throws SearchException {

		_elasticsearchUpdateDocumentCommand.updateDocument(
			documentType, searchContext, document);
	}

	protected void addDocuments(
			String documentType, SearchContext searchContext,
			Collection<Document> documents)
		throws SearchException {

		_elasticsearchUpdateDocumentCommand.updateDocuments(
			documentType, searchContext, documents);
	}

	@Override
	protected Document createDocument(
			long companyId, long groupId, String languageId, String keywords,
			float weight, String keywordFieldName, String typeFieldValue,
			int maxNGramLength)
		throws SearchException {

		Document document = super.createDocument(
			companyId, groupId, languageId, keywords, weight, keywordFieldName,
			typeFieldValue, maxNGramLength);

		document.remove(Field.LANGUAGE_ID);
		document.remove(keywordFieldName);

		String localizedName = DocumentImpl.getLocalizedName(
			languageId, keywordFieldName);

		document.addKeyword(localizedName, keywords);

		return document;
	}

	protected void deleteIndices(SearchContext searchContext, String indexType)
		throws Exception {

		ElasticsearchConnectionManager elasticsearchConnectionManager =
			ElasticsearchConnectionManager.getInstance();

		Client client = elasticsearchConnectionManager.getClient();

		DeleteByQueryRequestBuilder deleteByQueryRequestBuilder =
			client.prepareDeleteByQuery(
				String.valueOf(searchContext.getCompanyId()));

		deleteByQueryRequestBuilder.setQuery(QueryBuilders.matchAllQuery());
		deleteByQueryRequestBuilder.setTypes(indexType);

		Future<DeleteByQueryResponse> deleteByQueryRequestFuture =
			deleteByQueryRequestBuilder.execute();

		DeleteByQueryResponse deleteByQueryResponse =
			deleteByQueryRequestFuture.get();

		LogUtil.logActionResponse(_log, deleteByQueryResponse);
	}

	private static Log _log = LogFactoryUtil.getLog(
		ElasticsearchSpellCheckIndexWriter.class);

	private ElasticsearchUpdateDocumentCommand
		_elasticsearchUpdateDocumentCommand;

	private class _IndexWriterAdapter extends BaseIndexWriter {

		@Override
		public void addDocument(SearchContext searchContext, Document document)
			throws SearchException {

			doUpdateDocument(searchContext, document);
		}

		@Override
		public void addDocuments(
				SearchContext searchContext, Collection<Document> documents)
			throws SearchException {

			doUpdateDocuments(searchContext, documents);
		}

		@Override
		public void deleteDocument(SearchContext searchContext, String uid)
			throws SearchException {
		}

		@Override
		public void deleteDocuments(
			SearchContext searchContext, Collection<String> uids) {
		}

		@Override
		public void deletePortletDocuments(
			SearchContext searchContext, String portletId) {
		}

		@Override
		public void updateDocument(
				SearchContext searchContext, Document document)
			throws SearchException {

			doUpdateDocument(searchContext, document);
		}

		@Override
		public void updateDocuments(
				SearchContext searchContext, Collection<Document> documents)
			throws SearchException {

			doUpdateDocuments(searchContext, documents);
		}

		protected void doUpdateDocument(
				SearchContext searchContext, Document document)
			throws SearchException {

			if (searchContext == null) {
				searchContext = new SearchContext();

				long companyId = Long.parseLong(document.get(Field.COMPANY_ID));

				searchContext.setCompanyId(companyId);
			}

			String documentType = document.get(Field.TYPE);

			_elasticsearchUpdateDocumentCommand.updateDocument(
				documentType, searchContext, document);
		}

		protected void doUpdateDocuments(
				SearchContext searchContext, Collection<Document> documents)
			throws SearchException {

			if (documents.isEmpty()) {
				return;
			}

			Document document = documents.iterator().next();

			if (searchContext == null) {
				searchContext = new SearchContext();

				long companyId = Long.parseLong(document.get(Field.COMPANY_ID));

				searchContext.setCompanyId(companyId);
			}

			String documentType = document.get(Field.TYPE);

			_elasticsearchUpdateDocumentCommand.updateDocuments(
				documentType, searchContext, documents);
		}
	}

}