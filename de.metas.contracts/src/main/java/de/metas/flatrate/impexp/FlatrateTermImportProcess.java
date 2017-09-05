package de.metas.flatrate.impexp;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.adempiere.ad.persistence.TableModelLoader;
import org.adempiere.ad.trx.api.ITrx;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.impexp.AbstractImportProcess;
import org.adempiere.model.InterfaceWrapperHelper;
import org.adempiere.model.PlainContextAware;
import org.adempiere.util.Services;
import org.adempiere.util.lang.IMutable;
import org.compiere.model.I_AD_User;
import org.compiere.model.I_C_BPartner;
import org.compiere.model.I_M_Product;
import org.compiere.model.PO;
import org.compiere.util.DB;

import de.metas.flatrate.api.IFlatrateBL;
import de.metas.flatrate.model.I_C_Flatrate_Conditions;
import de.metas.flatrate.model.I_C_Flatrate_Term;
import de.metas.flatrate.model.I_I_Flatrate_Term;
import de.metas.flatrate.model.X_I_Flatrate_Term;

/*
 * #%L
 * de.metas.contracts
 * %%
 * Copyright (C) 2017 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

public class FlatrateTermImportProcess extends AbstractImportProcess<I_I_Flatrate_Term>
{
	private final transient IFlatrateBL flatrateBL = Services.get(IFlatrateBL.class);

	@Override
	public Class<I_I_Flatrate_Term> getImportModelClass()
	{
		return I_I_Flatrate_Term.class;
	}

	@Override
	public String getImportTableName()
	{
		return I_I_Flatrate_Term.Table_Name;
	}

	@Override
	protected String getTargetTableName()
	{
		return I_C_Flatrate_Term.Table_Name;
	}

	@Override
	protected String getImportOrderBySql()
	{
		return I_I_Flatrate_Term.COLUMNNAME_I_Flatrate_Term_ID;
	}

	@Override
	protected void updateAndValidateImportRecords()
	{
		final String trxName = ITrx.TRXNAME_ThreadInherited;
		final String sqlImportWhereClause = COLUMNNAME_I_IsImported + "<>" + DB.TO_BOOLEAN(true)
				+ "\n " + getWhereClause();

		//
		// Update C_BPartner_ID
		{
			final String sqlSelectByValue = "select MIN(bp." + I_C_BPartner.COLUMNNAME_C_BPartner_ID + ")"
					+ " from " + I_C_BPartner.Table_Name + " bp "
					+ " where bp." + I_C_BPartner.COLUMNNAME_Value + "=i." + I_I_Flatrate_Term.COLUMNNAME_BPartnerValue
					+ " and bp." + I_C_BPartner.COLUMNNAME_AD_Client_ID + "=i." + I_I_Flatrate_Term.COLUMNNAME_AD_Client_ID;
			final String sql = "UPDATE " + I_I_Flatrate_Term.Table_Name + " i "
					+ "\n SET " + I_I_Flatrate_Term.COLUMNNAME_C_BPartner_ID + "=(" + sqlSelectByValue + ")"
					+ "\n WHERE " + sqlImportWhereClause
					+ "\n AND i." + I_I_Flatrate_Term.COLUMNNAME_C_BPartner_ID + " IS NULL";

			final int no = DB.executeUpdateEx(sql, trxName);
			log.debug("Set C_BPartner_ID for {} records", no);

			// Flag missing BPartners
			markAsError("BPartner not found", I_I_Flatrate_Term.COLUMNNAME_C_BPartner_ID + " IS NULL"
					+ "\n AND " + sqlImportWhereClause);
		}

		//
		// Update C_Flatrate_Conditions_ID
		{
			final String sqlSelectByValue = "select MIN(fc." + I_C_Flatrate_Conditions.COLUMNNAME_C_Flatrate_Conditions_ID + ")"
					+ " from " + I_C_Flatrate_Conditions.Table_Name + " fc "
					+ " where fc." + I_C_Flatrate_Conditions.COLUMNNAME_Name + "=i." + I_I_Flatrate_Term.COLUMNNAME_C_Flatrate_Conditions_Value
					+ " and fc." + I_C_Flatrate_Conditions.COLUMNNAME_AD_Client_ID + "=i." + I_I_Flatrate_Term.COLUMNNAME_AD_Client_ID;
			final String sql = "UPDATE " + I_I_Flatrate_Term.Table_Name + " i "
					+ "\n SET " + I_I_Flatrate_Term.COLUMNNAME_C_Flatrate_Conditions_ID + "=(" + sqlSelectByValue + ")"
					+ "\n WHERE " + sqlImportWhereClause
					+ "\n AND i." + I_I_Flatrate_Term.COLUMNNAME_C_Flatrate_Conditions_ID + " IS NULL";

			final int no = DB.executeUpdateEx(sql, trxName);
			log.debug("Set C_Flatrate_Conditions_ID for {} records", no);

			// Flag missing flatrate conditions
			markAsError("Flatrate conditions not found", I_I_Flatrate_Term.COLUMNNAME_C_Flatrate_Conditions_ID + " IS NULL"
					+ "\n AND " + sqlImportWhereClause);
		}

		//
		// Update M_Product_ID by Value
		{
			final String sqlSelectByValue = "select MIN(bp." + I_M_Product.COLUMNNAME_M_Product_ID + ")"
					+ " from " + I_M_Product.Table_Name + " bp "
					+ " where bp." + I_M_Product.COLUMNNAME_Value + "=i." + I_I_Flatrate_Term.COLUMNNAME_ProductValue
					+ " and bp." + I_M_Product.COLUMNNAME_AD_Client_ID + "=i." + I_I_Flatrate_Term.COLUMNNAME_AD_Client_ID;
			final String sql = "UPDATE " + I_I_Flatrate_Term.Table_Name + " i "
					+ "\n SET " + I_I_Flatrate_Term.COLUMNNAME_M_Product_ID + "=(" + sqlSelectByValue + ")"
					+ "\n WHERE " + sqlImportWhereClause
					+ "\n AND i." + I_I_Flatrate_Term.COLUMNNAME_M_Product_ID + " IS NULL";

			final int no = DB.executeUpdateEx(sql, trxName);
			log.debug("Set M_Product_ID for {} records (by Value)", no);
		}
		// Update M_Product_ID by Name
		{
			final String sqlSelectByValue = "select MIN(bp." + I_M_Product.COLUMNNAME_M_Product_ID + ")"
					+ " from " + I_M_Product.Table_Name + " bp "
					+ " where bp." + I_M_Product.COLUMNNAME_Name + "=i." + I_I_Flatrate_Term.COLUMNNAME_ProductValue
					+ " and bp." + I_M_Product.COLUMNNAME_AD_Client_ID + "=i." + I_I_Flatrate_Term.COLUMNNAME_AD_Client_ID;
			final String sql = "UPDATE " + I_I_Flatrate_Term.Table_Name + " i "
					+ "\n SET " + I_I_Flatrate_Term.COLUMNNAME_M_Product_ID + "=(" + sqlSelectByValue + ")"
					+ "\n WHERE " + sqlImportWhereClause
					+ "\n AND i." + I_I_Flatrate_Term.COLUMNNAME_M_Product_ID + " IS NULL";

			final int no = DB.executeUpdateEx(sql, trxName);
			log.debug("Set M_Product_ID for {} records (by Name)", no);
			
		}
		// Flag missing product
		markAsError("Product not found", I_I_Flatrate_Term.COLUMNNAME_M_Product_ID + " IS NULL"
				+ "\n AND " + sqlImportWhereClause);
	}

	private final void markAsError(final String errorMsg, final String sqlWhereClause)
	{
		final String sql = "UPDATE " + I_I_Flatrate_Term.Table_Name
				+ "\n SET " + COLUMNNAME_I_IsImported + "=?, " + COLUMNNAME_I_ErrorMsg + "=" + COLUMNNAME_I_ErrorMsg + "||? "
				+ "\n WHERE " + sqlWhereClause;
		final Object[] sqlParams = new Object[] { X_I_Flatrate_Term.I_ISIMPORTED_ImportFailed, errorMsg + "; " };
		DB.executeUpdateEx(sql, sqlParams, ITrx.TRXNAME_ThreadInherited);

	}

	@Override
	protected I_I_Flatrate_Term retrieveImportRecord(final Properties ctx, final ResultSet rs) throws SQLException
	{
		final PO po = TableModelLoader.instance.getPO(ctx, I_I_Flatrate_Term.Table_Name, rs, ITrx.TRXNAME_ThreadInherited);
		return InterfaceWrapperHelper.create(po, I_I_Flatrate_Term.class);
	}

	@Override
	protected ImportRecordResult importRecord(final IMutable<Object> state, final I_I_Flatrate_Term importRecord) throws Exception
	{
		final I_C_Flatrate_Term contract = flatrateBL.createTerm(
				PlainContextAware.newWithThreadInheritedTrx(), // context
				importRecord.getC_BPartner(), // bpartner
				importRecord.getC_Flatrate_Conditions(), // conditions
				importRecord.getStartDate(), // startDate
				(I_AD_User)null, // userInCharge
				importRecord.getM_Product(), // product
				false // completeIt
		);
		if (contract == null)
		{
			throw new AdempiereException("contract not created");
		}

		if (importRecord.getEndDate() != null)
		{
			contract.setEndDate(importRecord.getEndDate());
		}

		//
		// Complete the subscription/contract
		InterfaceWrapperHelper.save(contract);
		flatrateBL.complete(contract);

		//
		importRecord.setC_Flatrate_Term(contract);
		return ImportRecordResult.Inserted;
	}

}
